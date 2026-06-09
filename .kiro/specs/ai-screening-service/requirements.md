# Requirements Document

## Introduction

This feature delivers the AI screening capability that verifies NGO applications on the Livana platform, together with the backend wiring that integrates it. It spans two codebases:

1. A NEW standalone Python service (FastAPI + LangGraph) located at the sibling folder `ai-screening/` (alongside `backend/` and `smartcontracts/`).
2. MODIFICATIONS to the existing Java Spring Boot backend at `backend/`.

When an NGO submits an application, the Backend triggers an asynchronous AI screening run. The AI_Screening_Service researches whether the organization exists and whether the official email is associated with that organization, optionally cross-references uploaded IPFS documents, and produces a confidence score, a human-readable research summary, and a PASS/FAIL verdict. The AI_Screening_Service returns those results to the Backend through an authenticated webhook callback. The Backend stores the results and advances the application to `PENDING_REVIEW` for human admin review.

The screening is best-effort: any AI failure (service unreachable, invalid Gemini key, timeout, or internal error) must fall through to `PENDING_REVIEW` with no AI results so an admin can still review manually. A flaky AI must never block a legitimate NGO.

The Gemini API key is admin-swappable, stored encrypted at rest in the Backend, never returned in plaintext, and forwarded to the stateless AI_Screening_Service per request.

This replaces the current synchronous fake advancement in `NgoApplicationService.submitApplication()` and the stub `AiScreeningService.triggerScreening()`.

Source of truth: PRD User Stories 8, 11 and the "NGO Application Module" / "AI Screening" sections (see `docs/backend_prd.md`), plus the locked design decisions provided for this feature.

## Glossary

- **Backend**: The existing Java Spring Boot application at `backend/` that owns the NGO application status machine, persistence, and admin APIs.
- **AI_Screening_Service**: The new stateless Python FastAPI application at `ai-screening/` that performs AI research and returns a verdict. It persists no Gemini key and no application data beyond a single request lifecycle.
- **NGO_Application**: A row in the `ngo_applications` table representing one NGO onboarding application, including its `status`, `ai_confidence_score`, `ai_research_summary`, and `ai_verdict` columns.
- **Application_Status**: The enum value of `NGO_Application.status`, one of `DRAFT`, `AI_SCREENING`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`.
- **Screening_Trigger**: The Backend component (the reworked `AiScreeningService.triggerScreening`) that sends application data to the AI_Screening_Service.
- **Screen_Endpoint**: The AI_Screening_Service HTTP endpoint `POST /screen` that accepts a screening request and returns HTTP 202 Accepted immediately.
- **Screening_Callback_Endpoint**: The Backend HTTP endpoint `POST /api/v1/internal/screening-callback` that receives screening results from the AI_Screening_Service.
- **Internal_Api_Path**: The Backend URL path prefix `/api/v1/internal/**` reserved for machine-to-machine callers and secured by Shared_Secret rather than Clerk JWT.
- **Shared_Secret**: A symmetric secret string, configured by environment variable on both the Backend and the AI_Screening_Service, used to authenticate machine-to-machine HTTP calls between them.
- **Callback_Secret_Header**: The HTTP header `X-Internal-Secret` that carries the Shared_Secret on a request to the Screening_Callback_Endpoint.
- **Screen_Secret_Header**: The HTTP header `X-Internal-Secret` that carries the Shared_Secret on a request to the Screen_Endpoint.
- **Ai_Config_Controller**: The Backend REST controller exposing admin Gemini-key configuration at base path `/api/v1/admin/ai-config`, restricted to the `ADMIN` role.
- **Set_Ai_Config_Endpoint**: The endpoint `POST /api/v1/admin/ai-config` used by an admin to set or replace the Gemini API key.
- **Get_Ai_Config_Endpoint**: The endpoint `GET /api/v1/admin/ai-config` that returns masked key metadata, never the raw key.
- **Ai_Config_Record**: A row in the new `ai_config` table that stores the encrypted Gemini API key and metadata (`set_by`, `set_at`).
- **Gemini_Api_Key**: The Google Generative AI API key supplied by an admin, used by the AI_Screening_Service to call Gemini.
- **Encryption_Key**: The symmetric key, provided to the Backend by environment variable, used to encrypt and decrypt the Gemini_Api_Key at rest.
- **Masked_Key**: A non-reversible display form of the Gemini_Api_Key showing only a short leading prefix and short trailing suffix (for example `AIza...3f9`), with all other characters replaced.
- **Research_Graph**: The LangGraph agent in the AI_Screening_Service composed of the nodes `extract_facts` → `research_existence` → `research_email_link` → (optional) `read_documents` → `synthesize` → `structured_verdict`.
- **Web_Search_Tool**: The Tavily-backed web search capability used by the Research_Graph to research organization existence and email association.
- **Documents_Cid**: The optional, nullable IPFS Content Identifier string supplied on an NGO_Application identifying uploaded supporting documents.
- **Ipfs_Gateway**: The HTTP IPFS gateway at base URL `https://gateway.pinata.cloud/ipfs/` used by the AI_Screening_Service to resolve a Documents_Cid.
- **Document_Fetch**: The AI_Screening_Service operation that resolves a Documents_Cid through the Ipfs_Gateway and retrieves document bytes.
- **Max_Document_Files**: The configured maximum number of files the AI_Screening_Service will fetch from a directory Documents_Cid.
- **Max_Document_Bytes**: The configured maximum byte size the AI_Screening_Service will fetch for any single document file.
- **Confidence_Score**: A decimal value in the inclusive range `0.00` to `100.00` (matching the DB column `ai_confidence_score DECIMAL(5,2)`) expressing the AI's confidence that the NGO is legitimate.
- **Research_Summary**: A human-readable text describing the AI's findings (matching the DB column `ai_research_summary TEXT`), shown to admins.
- **Verdict**: A value of exactly `PASS` or `FAIL` (matching the DB column `ai_verdict VARCHAR(10)`).
- **Screening_Result**: The triple of Confidence_Score, Research_Summary, and Verdict produced by one screening run.
- **Callback_Timeout_Window**: The configured maximum duration the Backend waits for a Screening_Callback after triggering screening before falling through to `PENDING_REVIEW`.

## Requirements

### Requirement 1: Asynchronous Screening Trigger on Submission

**User Story:** As an NGO operator, I want my application to start AI screening automatically when I submit it, so that verification begins without manual intervention.

#### Acceptance Criteria

1. WHEN an NGO_Application transitions from `DRAFT` to `AI_SCREENING` after Clerk email verification succeeds, THE Backend SHALL send a screening request to the Screen_Endpoint containing the application identifier, `orgName`, `registrationNumber`, `description`, `officialEmail`, and `documentsCid`.
2. WHEN the Screening_Trigger sends a screening request, THE Backend SHALL perform the send asynchronously so that the `submitApplication` response returns to the NGO operator without waiting for the screening research to complete.
3. WHEN the Screening_Trigger sends a screening request, THE Backend SHALL include the decrypted Gemini_Api_Key in the request to the Screen_Endpoint.
4. WHEN the Screening_Trigger sends a screening request, THE Backend SHALL include the Shared_Secret in the Screen_Secret_Header.
5. WHEN an NGO_Application enters `AI_SCREENING`, THE Backend SHALL leave the Application_Status at `AI_SCREENING` until either a screening callback is processed or the Callback_Timeout_Window elapses.
6. THE Backend SHALL remove the existing synchronous advancement in `submitApplication` that sets `AI_SCREENING` and then immediately sets `PENDING_REVIEW`.

### Requirement 2: AI Service Accepts and Acknowledges Screening Requests

**User Story:** As the Backend, I want the AI service to acknowledge screening requests immediately, so that the trigger call does not block on long-running research.

#### Acceptance Criteria

1. WHEN the Screen_Endpoint receives a request whose body contains the required fields (application identifier, `orgName`, `registrationNumber`, `description`, `officialEmail`) and a valid Shared_Secret, THE AI_Screening_Service SHALL respond with HTTP 202 Accepted before beginning research.
2. WHEN the Screen_Endpoint accepts a request, THE AI_Screening_Service SHALL perform the Research_Graph asynchronously after responding with HTTP 202.
3. IF a request to the Screen_Endpoint omits the Screen_Secret_Header or carries a value that does not equal the Shared_Secret, THEN THE AI_Screening_Service SHALL return HTTP 401 and SHALL NOT begin research.
4. IF a request to the Screen_Endpoint is missing a required field, THEN THE AI_Screening_Service SHALL return HTTP 422 and SHALL NOT begin research.
5. WHERE a request to the Screen_Endpoint omits `documentsCid` or sets it to null, THE AI_Screening_Service SHALL accept the request and perform web research without document cross-referencing.

### Requirement 3: AI Research Behavior — Organization Existence and Email Association

**User Story:** As an NGO operator, I want the AI to verify that my organization exists and that my official email is associated with it, so that verification is faster than pure manual review.

#### Acceptance Criteria

1. WHEN the Research_Graph runs for a screening request, THE AI_Screening_Service SHALL use the Web_Search_Tool to research whether a real, registered NGO matches the supplied `orgName` and `registrationNumber`.
2. WHEN the Research_Graph runs for a screening request, THE AI_Screening_Service SHALL use the Web_Search_Tool to research whether the supplied `officialEmail` is associated with the organization, including whether the email domain matches the organization's identified web presence.
3. WHEN the Research_Graph completes, THE AI_Screening_Service SHALL produce a Research_Summary that describes the existence findings and the email-association findings.
4. WHEN the Research_Graph completes, THE AI_Screening_Service SHALL produce a Confidence_Score in the inclusive range `0.00` to `100.00`.
5. WHEN the Research_Graph completes, THE AI_Screening_Service SHALL produce a Verdict equal to `PASS` or `FAIL`.

### Requirement 4: AI Research Behavior — Optional Document Cross-Referencing

**User Story:** As an admin, I want the AI to cross-reference uploaded documents against the application when documents are available, so that I get additional consistency signals without blocking applications that lack documents.

#### Acceptance Criteria

1. WHERE a screening request includes a non-null, non-blank Documents_Cid, THE AI_Screening_Service SHALL attempt a Document_Fetch through the Ipfs_Gateway.
2. WHEN a Document_Fetch returns readable documents, THE AI_Screening_Service SHALL read the documents with Gemini multimodal and cross-reference extracted facts (organization name, registration number, email domain) against the application fields.
3. WHEN document cross-referencing completes, THE AI_Screening_Service SHALL describe document findings in the Research_Summary as consistency with the application, using framing such as "consistent with the application," and SHALL NOT describe documents as "authentic" or "genuine."
4. THE AI_Screening_Service SHALL exclude document forgery detection and document authenticity determination from its analysis.
5. IF a Documents_Cid is absent, unresolvable, or unreadable, THEN THE AI_Screening_Service SHALL record that documents were not evaluated, continue with web research only, and produce a Screening_Result.
6. WHEN documents are absent, unresolvable, or unreadable, THE AI_Screening_Service SHALL NOT set the Verdict to `FAIL` solely because documents were not evaluated.
7. WHEN a Document_Fetch resolves to a directory Documents_Cid, THE AI_Screening_Service SHALL fetch at most Max_Document_Files files and SHALL fetch at most Max_Document_Bytes bytes per file.

### Requirement 5: Verdict Output Contract

**User Story:** As the Backend, I want the AI results to conform to the existing database column contract, so that results can be stored without transformation errors.

#### Acceptance Criteria

1. WHEN the AI_Screening_Service sends a Screening_Result to the Screening_Callback_Endpoint, THE AI_Screening_Service SHALL include the application identifier, the Confidence_Score, the Research_Summary, and the Verdict.
2. THE AI_Screening_Service SHALL emit the Confidence_Score as a number with at most two decimal places within the inclusive range `0.00` to `100.00`.
3. THE AI_Screening_Service SHALL emit the Verdict as exactly one of the string values `PASS` or `FAIL`.
4. IF the Research_Graph produces a structured output whose Verdict is not `PASS` or `FAIL`, or whose Confidence_Score is outside the inclusive range `0.00` to `100.00`, THEN THE AI_Screening_Service SHALL treat the run as failed regardless of any downstream validation and SHALL NOT send a Screening_Result containing the invalid values.
5. WHEN the AI_Screening_Service sends a Screening_Result, THE AI_Screening_Service SHALL include the Shared_Secret in the Callback_Secret_Header.

### Requirement 6: Callback Handling and State Advancement

**User Story:** As an admin, I want completed AI results stored on the application and the application advanced to review, so that I can make an informed approval decision.

#### Acceptance Criteria

1. WHEN the Screening_Callback_Endpoint receives an authenticated callback carrying a Screening_Result for an NGO_Application whose Application_Status is `AI_SCREENING`, THE Backend SHALL store the Confidence_Score into `ai_confidence_score`, the Research_Summary into `ai_research_summary`, and the Verdict into `ai_verdict`.
2. WHEN the Backend stores a Screening_Result for an NGO_Application in `AI_SCREENING`, THE Backend SHALL transition the Application_Status to `PENDING_REVIEW`.
3. WHEN the Screening_Callback_Endpoint processes a valid callback, THE Backend SHALL respond with HTTP 200.
4. WHEN the Screening_Callback_Endpoint receives an authenticated callback whose application identifier does not match any NGO_Application, THE Backend SHALL return HTTP 404 and SHALL NOT modify any NGO_Application.
5. WHILE processing a callback, THE Backend SHALL NOT transition an NGO_Application to `APPROVED` or `REJECTED`.

### Requirement 7: AI Failure Fallback to Manual Review

**User Story:** As an NGO operator, I want my application to still reach human review when the AI fails, so that a flaky AI never blocks my legitimate application.

#### Acceptance Criteria

1. IF the Screening_Trigger cannot deliver a screening request to the Screen_Endpoint because of a connection error, DNS error, timeout, or a non-2xx response, THEN THE Backend SHALL transition the NGO_Application from `AI_SCREENING` to `PENDING_REVIEW` with `ai_confidence_score`, `ai_research_summary`, and `ai_verdict` left null.
2. IF no callback is processed for an NGO_Application within the Callback_Timeout_Window after it enters `AI_SCREENING`, THEN THE Backend SHALL transition that NGO_Application to `PENDING_REVIEW` with `ai_confidence_score`, `ai_research_summary`, and `ai_verdict` left null.
3. IF the Backend cannot decrypt or load a Gemini_Api_Key when triggering screening for an NGO_Application that is in `AI_SCREENING`, THEN THE Backend SHALL transition that NGO_Application to `PENDING_REVIEW` with `ai_confidence_score`, `ai_research_summary`, and `ai_verdict` left null.
4. IF the AI_Screening_Service encounters an internal error, an invalid Gemini_Api_Key, or a research failure during a run, THEN THE AI_Screening_Service SHALL leave the NGO_Application to reach `PENDING_REVIEW` through the Backend fallback rather than sending a Screening_Result with fabricated values.
5. WHEN the Backend applies a fallback transition to `PENDING_REVIEW` (a transition that leaves the AI result fields null), THE Backend SHALL produce that fallback transition exactly once for a given NGO_Application even if both the trigger failure path and the timeout path occur.

### Requirement 8: Callback Authentication

**User Story:** As the platform, I want the screening callback authenticated by a machine secret, so that only the AI service can write screening results.

#### Acceptance Criteria

1. THE Backend SHALL expose the Screening_Callback_Endpoint under the Internal_Api_Path and SHALL authenticate it using the Shared_Secret rather than the Clerk JWT filter.
2. IF a request to the Screening_Callback_Endpoint omits the Callback_Secret_Header or carries a value that does not equal the Shared_Secret, THEN THE Backend SHALL return HTTP 401 and SHALL NOT modify any NGO_Application.
3. WHEN the Backend compares the Callback_Secret_Header value to the Shared_Secret, THE Backend SHALL use a constant-time comparison.
4. THE Backend SHALL exclude the Internal_Api_Path from Clerk JWT authentication so that a machine caller without a Clerk JWT can reach the Screening_Callback_Endpoint.

### Requirement 9: Callback Idempotency

**User Story:** As the platform, I want repeated callbacks to be safe, so that a duplicate delivery cannot corrupt application state.

#### Acceptance Criteria

1. WHEN the Screening_Callback_Endpoint receives a callback for an NGO_Application whose Application_Status is already `PENDING_REVIEW`, `APPROVED`, or `REJECTED`, THE Backend SHALL respond with HTTP 200 and SHALL NOT modify the Application_Status or the stored AI result fields.
2. WHEN two authenticated callbacks carrying a Screening_Result arrive for the same NGO_Application, THE Backend SHALL apply the result and advance the status on the first callback processed while the application is in `AI_SCREENING`, and SHALL leave state unchanged for the second callback.
3. THE Backend SHALL accept a Screening_Result write only while the target NGO_Application is in `AI_SCREENING`.

### Requirement 10: Admin Sets the Gemini API Key

**User Story:** As an admin, I want to set or replace the Gemini API key through an admin endpoint, so that the platform can use a current, valid key without redeployment.

#### Acceptance Criteria

1. WHEN an authenticated admin sends a request to the Set_Ai_Config_Endpoint with a non-blank Gemini_Api_Key in the request body, THE Backend SHALL encrypt the Gemini_Api_Key with the Encryption_Key and store the ciphertext in the Ai_Config_Record.
2. WHEN the Backend stores a Gemini_Api_Key, THE Backend SHALL record the requesting admin identity in `set_by` and the timestamp in `set_at` of the Ai_Config_Record.
3. WHEN an admin sets a Gemini_Api_Key while an Ai_Config_Record already exists, THE Backend SHALL replace the stored ciphertext and update `set_by` and `set_at`.
4. IF a request to the Set_Ai_Config_Endpoint carries a blank or missing Gemini_Api_Key, THEN THE Backend SHALL return HTTP 400 with error code `VALIDATION_ERROR` and SHALL NOT modify the Ai_Config_Record.
5. WHEN the Set_Ai_Config_Endpoint succeeds, THE Backend SHALL respond without including the raw Gemini_Api_Key in the response body.
6. WHEN the Backend decrypts a stored ciphertext using the Encryption_Key, THE Backend SHALL recover the exact Gemini_Api_Key that was stored (encryption round-trip).

### Requirement 11: Admin Reads Masked Key Metadata

**User Story:** As an admin, I want to see that a key is configured and who set it, without exposing the raw key, so that I can audit configuration safely.

#### Acceptance Criteria

1. WHEN an authenticated admin sends a request to the Get_Ai_Config_Endpoint and an Ai_Config_Record exists, THE Backend SHALL respond with HTTP 200 containing the Masked_Key, the `set_by` value, and the `set_at` value.
2. THE Backend SHALL produce the Masked_Key showing only a leading prefix and a trailing suffix of the Gemini_Api_Key, with all intervening characters replaced by a fixed mask.
3. THE Backend SHALL NOT include the raw Gemini_Api_Key in any response from the Get_Ai_Config_Endpoint.
4. WHEN an authenticated admin sends a request to the Get_Ai_Config_Endpoint and no Ai_Config_Record exists, THE Backend SHALL respond with HTTP 200 indicating that no key is configured and SHALL NOT include a Masked_Key value.

### Requirement 12: Gemini Key Confidentiality

**User Story:** As the platform, I want the Gemini key protected at rest and in transit, so that a leak of the database or logs does not expose it.

#### Acceptance Criteria

1. THE Backend SHALL store the Gemini_Api_Key only as ciphertext encrypted with the Encryption_Key, and SHALL NOT store the Gemini_Api_Key as plaintext in any database column, file, or cache.
2. THE Backend SHALL NOT write the raw Gemini_Api_Key to any log statement, exception message, or error response body.
3. THE Backend SHALL NOT return the raw Gemini_Api_Key in any API response.
4. WHEN the Backend forwards the Gemini_Api_Key to the Screen_Endpoint, THE Backend SHALL transmit it only in the screening request to the AI_Screening_Service.
5. THE AI_Screening_Service SHALL use a supplied Gemini_Api_Key only for the run that supplied it, and SHALL discard the Gemini_Api_Key when the run ends.
6. THE AI_Screening_Service SHALL NOT persist the Gemini_Api_Key or the application data to any storage beyond the request lifecycle.

### Requirement 13: AI Config Endpoint Authorization

**User Story:** As the platform, I want only admins to manage the Gemini key, so that non-admin users cannot read or change AI configuration.

#### Acceptance Criteria

1. IF a request to the Set_Ai_Config_Endpoint or the Get_Ai_Config_Endpoint omits a valid Clerk JWT, THEN THE Backend SHALL return HTTP 401.
2. IF a request to the Set_Ai_Config_Endpoint or the Get_Ai_Config_Endpoint carries a valid Clerk JWT for a user whose role is not `ADMIN`, THEN THE Backend SHALL return HTTP 403.
3. WHEN a request to the Set_Ai_Config_Endpoint or the Get_Ai_Config_Endpoint carries a valid Clerk JWT for a user whose role is `ADMIN`, THE Ai_Config_Controller SHALL process the request and respond with a 2xx status when the operation succeeds.

### Requirement 14: Persistence Schema for AI Configuration

**User Story:** As the platform, I want a database table for the encrypted Gemini key created through the standard migration tooling, so that configuration survives restarts and deployments.

#### Acceptance Criteria

1. THE Backend SHALL create the `ai_config` table through a Flyway migration under `classpath:db/migration` that follows the existing versioned naming convention.
2. THE `ai_config` table SHALL include a column for the encrypted Gemini_Api_Key ciphertext, a `set_by` column, a `set_at` timestamp column, and a primary key.
3. THE Backend SHALL store at most one active Ai_Config_Record representing the current Gemini_Api_Key.
4. IF an attempt is made to insert a second active Ai_Config_Record while one already exists, THEN THE Backend SHALL reject the insertion and SHALL preserve the existing Ai_Config_Record.

### Requirement 15: Cross-Codebase Integration Contract

**User Story:** As a developer working across both codebases, I want a single agreed request and response contract, so that the Backend and AI service interoperate without ambiguity.

#### Acceptance Criteria

1. THE Backend and the AI_Screening_Service SHALL exchange the screening request and the Screening_Result using a shared JSON contract whose fields are the application identifier, `orgName`, `registrationNumber`, `description`, `officialEmail`, `documentsCid`, Confidence_Score, Research_Summary, and Verdict.
2. THE AI_Screening_Service SHALL be deployable as an independent service at `ai-screening/`, separate from the Backend.
3. THE AI_Screening_Service SHALL read its callback target URL and Shared_Secret from configuration so that the deployment environment determines where results are sent.
4. THE Backend SHALL read the Screen_Endpoint base URL, the Shared_Secret, the Encryption_Key, and the Callback_Timeout_Window from configuration.
5. WHEN the AI_Screening_Service sends a Screening_Result to the Screening_Callback_Endpoint, THE AI_Screening_Service SHALL address the configured callback URL using the application identifier from the originating screening request.
6. IF the AI_Screening_Service cannot resolve the configured callback URL for an application identifier, THEN THE AI_Screening_Service SHALL NOT send the Screening_Result.
