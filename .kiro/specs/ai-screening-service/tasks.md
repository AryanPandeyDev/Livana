# Implementation Plan: AI Screening Service

## Overview

This feature spans **two codebases** and the task list is grouped accordingly:

- **Backend (`backend/`)** — Java 17 / Spring Boot 4. Authored **and verified** in this
  environment via `./mvnw compile` and `./mvnw test`. jqwik 1.9.1 and Testcontainers are
  available for property and integration tests.
- **AI service (`ai-screening/`)** — a NEW Python 3.11+ / FastAPI / LangGraph service managed
  with `uv`. This environment has **no Python toolchain**, so every Python task only **authors**
  the code; the user **runs and verifies** it on their own machine (`uv sync`, `uv run pytest`,
  `uv run uvicorn ...`). Each Python task repeats this note.

The async webhook-callback contract (`ScreeningRequest` / `ScreeningCallbackRequest` and their
Pydantic mirrors) is the seam between the two codebases. Once the backend contract DTOs exist,
the Python chain can be authored in parallel with the remaining backend work.

Implementation order: backend config/encryption/migration foundation → services → controllers →
security/callback → timeout sweep → backend checkpoints; then the AI service scaffold → models →
IPFS → graph → callback → app → docs → Docker Compose → Python handoff checkpoint.

Test sub-tasks are marked optional with `*`. Each property-test sub-task names its Property number
(from the design's Correctness Properties section) and the requirements it validates.

## Tasks

- [x] 1. Backend configuration foundation
  - [x] 1.1 Implement properties and async/scheduling enablement
    - Create `AiScreeningProperties` (`@ConfigurationProperties(prefix = "ai-screening")`) binding
      `base-url`, `shared-secret`, and `callback-timeout-seconds`, mirroring the existing
      `IndexerProperties` style
    - Add the four properties to `backend/src/main/resources/application.properties`:
      `ai-screening.base-url`, `ai-screening.shared-secret`,
      `ai-screening.callback-timeout-seconds`, and `ai-config.encryption-key` (all env-backed)
    - Add `@EnableAsync` (new `AsyncConfig` class or on `BackendApplication`; `@EnableScheduling`
      already present) so the async trigger runs off the request thread
    - _Requirements: 1.2, 7.2, 15.4_

  - [ ]* 1.2 Write smoke test for property binding
    - Assert `AiScreeningProperties` binds all `ai-screening.*` values from configuration
    - _Requirements: 15.4_

- [x] 2. Backend key encryption
  - [x] 2.1 Implement `KeyEncryptionService` (AES-GCM)
    - Define the interface (`encrypt`, `decrypt`, `mask`) and an implementation using
      `javax.crypto` AES-GCM with a 256-bit key read from `ai-config.encryption-key` (base64)
    - Store `base64(iv || ciphertext+tag)` with a fresh random 12-byte IV per `encrypt`
    - Masking keeps the leading 4 + trailing 3 characters, replacing the middle with a fixed mask;
      shorter keys are fully masked
    - Never log the plaintext key
    - _Requirements: 10.1, 10.5, 10.6, 11.2, 11.3, 12.1, 12.3_

  - [ ]* 2.2 Write property test for encryption round-trip
    - **Property 1: Gemini key encryption round-trip**
    - jqwik `@Property` (≥100 iterations) over non-blank strings: assert
      `decrypt(encrypt(k)).equals(k)` and `!encrypt(k).equals(k)`
    - **Validates: Requirements 10.1, 10.6, 12.1**

  - [ ]* 2.3 Write property test for key masking
    - **Property 2: Masking never reveals the raw key**
    - jqwik `@Property` (≥100 iterations) over keys longer than prefix+suffix: assert the masked
      output keeps at most the prefix/suffix originals and never contains the raw key as a substring
    - **Validates: Requirements 10.5, 11.2, 11.3, 12.3**

- [x] 3. Backend ai_config persistence
  - [x] 3.1 Create Flyway migration `V3__ai_config.sql`
    - Add to `backend/src/main/resources/db/migration` following the `V1`/`V2` convention
    - Create `ai_config` with `id` (UUID PK), `encrypted_key` TEXT, `set_by`, `set_at` TIMESTAMPTZ,
      `is_active` BOOLEAN, plus a partial unique index `uq_ai_config_active` on `is_active` WHERE
      `is_active = true`
    - _Requirements: 14.1, 14.2, 14.3, 14.4_

  - [x] 3.2 Create `AiConfig` entity and `AiConfigRepository`
    - Map the entity to `ai_config`; add a Spring Data repository with a finder for the active record
    - _Requirements: 14.2, 14.3_

  - [ ]* 3.3 Write migration smoke/integration test (Testcontainers)
    - Assert Flyway `V3` creates `ai_config` with the required columns on Postgres, and that
      inserting a second active row is rejected by the partial unique index
    - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 4. Backend AiConfigService and DTOs
  - [x] 4.1 Create AI config DTOs
    - `SetAiConfigRequest` (`@NotBlank geminiApiKey`) and
      `AiConfigStatusResponse` (`configured`, `maskedKey`, `setBy`, `setAt`)
    - _Requirements: 10.5, 11.1_

  - [x] 4.2 Implement `AiConfigService`
    - `setKey` (encrypt + in-place replace of the single active row, recording `set_by`/`set_at`),
      `getStatus` (masked metadata, no-record case), and `getDecryptedKey` (Optional)
    - _Requirements: 10.1, 10.2, 10.3, 11.1, 11.4, 12.1, 12.3_

  - [ ]* 4.3 Write unit tests for `AiConfigService`
    - Cover `set_by`/`set_at` recording, replace semantics, masked status, no-record status,
      and that a second active record is not created
    - _Requirements: 10.2, 10.3, 11.1, 11.4, 14.4_

- [x] 5. Backend AiConfigController
  - [x] 5.1 Implement `AiConfigController` (ADMIN-only)
    - `POST`/`GET /api/v1/admin/ai-config` guarded by `@PreAuthorize("hasRole('ADMIN')")`;
      response never includes the raw key
    - _Requirements: 10.1, 10.4, 10.5, 11.1, 11.4, 13.3_

  - [ ]* 5.2 Write MockMvc tests for AI config endpoints
    - No JWT → 401, non-admin → 403, admin → 2xx; blank key → 400 `VALIDATION_ERROR`;
      set/replace and masked GET / no-record GET behavior
    - _Requirements: 10.4, 11.1, 11.4, 13.1, 13.2, 13.3_

- [x] 6. Backend screening contract DTOs
  - [x] 6.1 Create screening request/callback DTOs
    - Create `ScreeningRequest` (`applicationId`, `orgName`, `registrationNumber`, `description`,
      `officialEmail`, `documentsCid`, `geminiApiKey`, plus `withGeminiKey(...)`) and
      `ScreeningCallbackRequest` with bean validation (verdict `PASS|FAIL`, score
      `[0.00, 100.00]` with `@Digits(integer=3, fraction=2)`, `@NotBlank researchSummary`)
    - This is the cross-codebase JSON contract; the Python models mirror it
    - _Requirements: 5.1, 5.2, 5.3, 6.1, 15.1_

- [ ] 7. Backend repository conditional transitions
  - [-] 7.1 Add conditional update and stale-finder queries
    - Add `@Modifying` queries to `NgoApplicationRepository`: `completeScreening`
      (`UPDATE ... SET status, score, summary, verdict WHERE id = :id AND status = :fromStatus`),
      `fallbackToPendingReview` (status-only conditional update), and
      `findStaleScreeningIds(status, threshold)` for the timeout sweep
    - The rows-affected count is the single concurrency primitive for exactly-once advancement
    - _Requirements: 6.1, 6.2, 7.2, 7.5, 9.2, 9.3_

- [x] 8. Backend NgoApplicationService rework
  - [x] 8.1 Rework submit flow and add transition methods
    - Add `completeAiScreening` and `fallbackToManualReview` (both delegate to the conditional
      updates and return the rows-affected winner signal), `applicationExists`, and
      `toScreeningRequest`
    - Remove the fake synchronous `AI_SCREENING → PENDING_REVIEW` advancement in
      `submitApplication`; after persisting `AI_SCREENING`, call the async trigger and return
    - _Requirements: 1.1, 1.6, 6.1, 6.2, 6.4, 6.5, 7.1, 7.3, 7.5, 9.1, 9.2, 9.3_

  - [ ]* 8.2 Write property test for exactly-once advancement (Testcontainers)
    - **Property 6: Screening advancement is exactly-once and idempotent**
    - jqwik `@Property` (≥100 iterations) generating arbitrary interleavings of
      `completeAiScreening` / `fallbackToManualReview` / sweep operations against a seeded
      `AI_SCREENING` row on Testcontainers Postgres; assert exactly one transition wins, later
      operations are no-ops, and status never becomes `APPROVED`/`REJECTED`
    - **Validates: Requirements 6.1, 6.2, 6.5, 7.5, 9.1, 9.2, 9.3**

  - [ ]* 8.3 Write example test for immediate submit response
    - Assert `submitApplication` returns with status `AI_SCREENING` and performs no synchronous
      advancement to `PENDING_REVIEW`
    - _Requirements: 1.2, 1.5, 1.6_

- [x] 9. Backend async screening trigger
  - [x] 9.1 Rework `AiScreeningService.triggerScreening`
    - Make it `@Async`; load + decrypt the Gemini key (fallback to manual review if absent/
      undecryptable), then `POST /screen` via `java.net.http.HttpClient` with `X-Internal-Secret`
      and the decrypted key in the body; on connect error, timeout, or non-2xx call
      `fallbackToManualReview`. Never log the key
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 7.1, 7.3, 12.4_

  - [ ]* 9.2 Write example tests for the trigger
    - Mock `HttpClient`: assert a well-formed `/screen` request with all app fields, decrypted key,
      and secret header; assert fallback to `PENDING_REVIEW` (null results) on throw, non-2xx, and
      key-load failure
    - _Requirements: 1.1, 1.3, 1.4, 7.1, 7.3, 12.4_

- [x] 10. Backend internal API security and callback
  - [x] 10.1 Implement internal-path security
    - Add `InternalSecretFilter` (reads `X-Internal-Secret`, constant-time `MessageDigest.isEqual`
      compare, writes a 401 `ErrorResponse` on mismatch) and `InternalApiSecurityConfig`
      (`@Order(1)` `SecurityFilterChain` matched to `/api/v1/internal/**`, no Clerk JWT); annotate
      the existing chain `@Order(2)`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 10.2 Implement `AiScreeningCallbackController`
    - `POST /api/v1/internal/screening-callback`: 404 when the application is unknown, otherwise
      delegate to `completeAiScreening` and return 200 (idempotent whether it won or was already
      advanced); never transition to `APPROVED`/`REJECTED`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 9.1_

  - [ ]* 10.3 Write property test for callback secret rejection
    - **Property 7: Internal endpoints reject every non-matching secret** (backend side)
    - jqwik `@Property` (≥100 iterations) over header values ≠ secret (absent, empty, near-match)
      via MockMvc; assert 401 and no application mutation
    - **Validates: Requirements 8.2, 8.3**

  - [ ]* 10.4 Write example/edge tests for the callback
    - Happy path → 200 with score/summary/verdict stored; unknown appId → 404 with no mutation;
      internal path reachable without a Clerk JWT when the secret is valid; malformed body → 400
    - _Requirements: 6.1, 6.3, 6.4, 8.1, 8.4_

- [x] 11. Backend screening timeout sweep
  - [x] 11.1 Implement `ScreeningTimeoutSweep`
    - `@Scheduled` job that loads stale `AI_SCREENING` ids past the callback-timeout window via
      `findStaleScreeningIds` and calls `fallbackToManualReview` for each
    - _Requirements: 7.2, 7.5_

  - [ ]* 11.2 Write example test for the sweep
    - Seed a stale `AI_SCREENING` row; assert the sweep advances it to `PENDING_REVIEW` with null
      AI result fields
    - _Requirements: 7.2_

- [x] 12. Checkpoint - Backend compile
  - Run `./mvnw compile`. Ensure the backend compiles cleanly; ask the user if questions arise.

- [x] 13. Checkpoint - Backend tests
  - Run `./mvnw test`. Ensure all backend unit, property, and integration tests pass; ask the user
    if questions arise.

- [x] 14. AI service project scaffold
  - [x] 14.1 Create project scaffold and config
    - Create `ai-screening/` with `pyproject.toml` (uv-managed: FastAPI, LangGraph,
      langchain-google-genai, httpx, pydantic, pytest, hypothesis), the `ai_screening/` package
      layout, and `config.py` reading env settings (`BACKEND_CALLBACK_URL`, `SHARED_SECRET`,
      `IPFS_GATEWAY_URL`, `MAX_DOCUMENT_FILES`, `MAX_DOCUMENT_BYTES`, `GEMINI_MODEL`,
      `TAVILY_API_KEY`)
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 15.2, 15.3_

- [x] 15. AI service data models
  - [x] 15.1 Implement `models.py`
    - Pydantic models `ScreenRequest`, `StructuredVerdict` (`confidence_score` ge=0/le=100,
      `verdict` Literal[PASS, FAIL]), `CallbackPayload`, and the `ResearchState` TypedDict; use
      JSON aliases matching the backend contract; round `confidence_score` to two decimals before
      building `CallbackPayload`
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 2.4, 5.2, 5.3, 15.1_

  - [ ]* 15.2 Write property test for well-formed verdict
    - **Property 3: Emitted verdict is always well-formed**
    - hypothesis (≥100 examples): for valid raw scores/verdicts assert `CallbackPayload` score is in
      `[0.00, 100.00]` with ≤2 decimals and verdict ∈ {PASS, FAIL}
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - **Validates: Requirements 3.4, 3.5, 5.2, 5.3**

- [x] 16. AI service IPFS document fetch
  - [x] 16.1 Implement `ipfs.py`
    - `Document_Fetch` via httpx for single-file and directory CIDs through the Ipfs_Gateway, with
      `MAX_DOCUMENT_FILES` and `MAX_DOCUMENT_BYTES` caps and guards for unresolvable/unreadable CIDs
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 4.1, 4.5, 4.7_

  - [ ]* 16.2 Write property test for directory fetch caps
    - **Property 5: Directory document fetch respects caps**
    - hypothesis (≥100 examples): generate directory listings of N files with random sizes; assert
      files fetched ≤ `MAX_DOCUMENT_FILES` and bytes per file ≤ `MAX_DOCUMENT_BYTES`
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - **Validates: Requirements 4.7**

  - [ ]* 16.3 Write unit tests for IPFS fetch variants
    - Single-file, directory, unresolvable (404), and unreadable cases → `documents_evaluated=false`
      and the run still completes
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 4.1, 4.5, 4.6_

- [x] 17. AI service LangGraph research graph
  - [x] 17.1 Implement `graph.py`
    - Build `Research_Graph`: `extract_facts` → `research_existence` → `research_email_link` →
      (conditional) `read_documents` → `synthesize` → `structured_verdict`; Gemini via
      langchain-google-genai using the per-request key, Tavily web search; document findings framed
      as "consistent with the application" (never "authentic"/"genuine"); the conditional edge skips
      `read_documents` when `documentsCid` is absent/unresolvable and never drives a `FAIL` on its own
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ]* 17.2 Write unit tests for graph node wiring
    - Mock Tavily/Gemini: assert existence + email-link searches are invoked, the research summary
      is populated, and document findings use "consistent" framing rather than "authentic"
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 3.1, 3.2, 3.3, 4.2, 4.3, 4.4_

- [x] 18. AI service callback client
  - [x] 18.1 Implement `callback.py`
    - httpx `POST` of `CallbackPayload` to the configured backend callback URL with the
      `X-Internal-Secret` header; if the callback URL is unresolved, log and send nothing
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 5.1, 5.5, 15.5, 15.6_

  - [ ]* 18.2 Write unit tests for the callback client
    - Assert the payload carries all fields and the secret header (respx/mock transport); assert an
      unresolved callback URL results in no send
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 5.1, 5.5, 15.6_

- [x] 19. AI service FastAPI app and orchestration
  - [x] 19.1 Implement `main.py`
    - `POST /screen` returns 202 and schedules `run_screening` via `BackgroundTasks`;
      `verify_internal_secret` dependency uses `hmac.compare_digest` (401 on mismatch); add a health
      check; `run_screening` builds/runs the graph and calls the callback client only on success with
      a valid verdict, failing silently (log only) on any error so the backend timeout fallback
      handles the application; the Gemini key lives only as a local variable per run
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 7.4, 12.5, 12.6, 15.3_

  - [ ]* 19.2 Write property test for /screen secret rejection
    - **Property 7: Internal endpoints reject every non-matching secret** (AI side)
    - hypothesis (≥100 examples) over header values ≠ secret via `TestClient`; assert 401 and that
      `run_screening` is never scheduled
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - **Validates: Requirements 2.3**

  - [ ]* 19.3 Write property test for application-id preservation
    - **Property 8: Application identifier is preserved end-to-end**
    - hypothesis (≥100 examples): for any request appId, run the graph with mocked Gemini/Tavily and
      assert the callback payload's `applicationId` equals the request's
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - **Validates: Requirements 15.5**

  - [ ]* 19.4 Write property test for failed/invalid runs sending nothing
    - **Property 4: Invalid or failed runs send nothing**
    - hypothesis (≥100 examples): generate out-of-range scores / non-PASS|FAIL verdicts and forced
      node exceptions; assert `StructuredVerdict` validation rejects them and the callback client is
      never invoked
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - **Validates: Requirements 5.4, 7.4**

  - [ ]* 19.5 Write example tests for the app
    - `/screen` returns 202 and schedules research; missing field → 422; null/absent `documentsCid`
      accepted (web-only); config loads from env
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 15.3_

- [x] 20. AI service documentation
  - [x] 20.1 Write `ai-screening/README.md`
    - Document `uv sync`, `uv run pytest`, and
      `uv run uvicorn ai_screening.main:app --port 8000` commands plus all required env vars
    - NOTE: authored in this environment; run/verified by the user on their machine (no Python
      toolchain here)
    - _Requirements: 15.2, 15.3_

- [x] 21. Docker Compose integration
  - [x] 21.1 Add `ai-screening` service to Docker Compose
    - Add an `ai-screening` service alongside `backend` and `postgres` for local development,
      wiring the shared secret, callback URL, IPFS gateway, and model env vars
    - NOTE: the compose file is authored here; the user builds/runs it on their machine (no Python
      toolchain here)
    - _Requirements: 15.2_

- [x] 22. Checkpoint - Python service handoff
  - The Python service was authored in this environment but not executed here. The user must run
    `uv sync` and `uv run pytest` in `ai-screening/` on their machine to install dependencies and
    verify all property and example tests, then `uv run uvicorn ai_screening.main:app --port 8000`
    for a manual smoke run. Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional test sub-tasks and can be skipped for a faster MVP.
- Backend tasks (1–11) are authored and verified in this environment; the compile checkpoint (12)
  and test checkpoint (13) gate that verification.
- AI service tasks (14–21) are authored here only — there is no Python toolchain in this
  environment. Every Python task notes that the user runs and verifies it locally; the handoff
  checkpoint (22) records the exact commands.
- Each task references specific requirements for traceability; property-test sub-tasks additionally
  name their Property number and the requirements that property validates.
- Property tests cover universal behavior (encryption round-trip, masking, verdict bounds,
  exactly-once advancement, secret rejection, fetch caps, appId preservation); unit/example/edge
  tests cover concrete scenarios and external-service wiring.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "3.1", "4.1", "6.1", "7.1", "14.1"] },
    { "id": 1, "tasks": ["1.2", "2.2", "2.3", "3.2", "8.1", "10.1", "15.1", "16.1"] },
    { "id": 2, "tasks": ["3.3", "4.2", "8.2", "8.3", "10.2", "11.1", "15.2", "16.2", "16.3", "17.1", "18.1"] },
    { "id": 3, "tasks": ["4.3", "5.1", "9.1", "10.3", "10.4", "11.2", "17.2", "18.2", "19.1"] },
    { "id": 4, "tasks": ["5.2", "9.2", "19.2", "19.3", "19.4", "19.5", "20.1", "21.1"] }
  ]
}
```
