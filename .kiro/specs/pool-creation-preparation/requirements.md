# Requirements Document

## Introduction

Build the backend endpoints that a verified NGO uses BEFORE calling `deployPool()` on-chain. The NGO submits a cover image and pool metadata through the backend; the backend uploads the image and metadata JSON to IPFS using the NGO's own Pinata API key (passed in request headers, never persisted), then returns the metadata CID that the frontend will pass to `deployPool(metadataCid)`.

The metadata JSON produced by this feature must pass the validation rules already implemented in `com.livana.backend.indexer.service.IpfsMetadataService`. Otherwise the indexer will silently drop the pool when the on-chain `PoolDeployed` event later fires, and the pool will never appear on the platform.

This feature does not store pool data in the database, does not call any on-chain function, and does not validate on-chain state. Those responsibilities belong to the frontend (deployPool call) and the indexer (PoolDeployed event handling).

Source of truth: PRD User Stories 15-18 and PRD lines 78, 108, 159, 181 (see `docs/backend_prd.md`).

## Glossary

- **Pool_Creation_API**: The REST controller that exposes the pool creation preparation endpoints at base path `/api/v1/pools`.
- **Image_Upload_Endpoint**: The endpoint `POST /api/v1/pools/upload-image` exposed by the Pool_Creation_API. Accepts a `multipart/form-data` request with a single file part named `file`.
- **Prepare_Endpoint**: The endpoint `POST /api/v1/pools/prepare` exposed by the Pool_Creation_API. Accepts an `application/json` body conforming to the Pool_Metadata_Schema.
- **Pinata_Client**: An internal Spring component that calls Pinata's REST API. It is the only component allowed to read the NGO's Pinata API key from request headers.
- **Pinata_Pin_File_Url**: The Pinata endpoint `https://api.pinata.cloud/pinning/pinFileToIPFS` used to upload binary files.
- **Pinata_Pin_Json_Url**: The Pinata endpoint `https://api.pinata.cloud/pinning/pinJSONToIPFS` used to upload JSON documents.
- **Pinata_Api_Key_Header**: The HTTP request header `X-Pinata-Api-Key` that carries the NGO's Pinata API key value.
- **Pinata_Secret_Api_Key_Header**: The HTTP request header `X-Pinata-Secret-Api-Key` that carries the NGO's Pinata secret API key value.
- **Pinata_Headers**: Both Pinata_Api_Key_Header and Pinata_Secret_Api_Key_Header sent together on a single request.
- **IPFS_CID**: A Pinata-issued IPFS Content Identifier string (the `IpfsHash` field returned by Pinata).
- **Pool_Metadata_Schema**: The JSON object structure `{ title: string (required, non-blank), description: string (required, non-blank), region: string (required, non-blank), coverImage: string (optional, IPFS CID), targetAmount: long (required, > 0, raw USDC with 6 decimals) }`. This shape and these validation rules are identical to those enforced by `IpfsMetadataService.parseAndValidate`.
- **Pool_Metadata_Json**: A JSON document conforming to the Pool_Metadata_Schema. This is the document that the prepare flow pins to IPFS — i.e. the bytes stored at the returned IPFS_CID and read back by `IpfsMetadataService` when the indexer processes a `PoolDeployed` event.
- **Pinata_Json_Envelope**: The wire format Pinata's `pinJSONToIPFS` endpoint requires for the request body: a JSON object with a single field `pinataContent` whose value is the document to pin. The document Pinata stores at the returned CID is the value of `pinataContent`, NOT the envelope. The Prepare_Endpoint produces a Pool_Metadata_Json and wraps it in a Pinata_Json_Envelope only on the wire to Pinata.
- **USDC_Raw_Amount**: A `long` value representing a USDC amount with 6 decimal places (1 USDC = 1000000).
- **Verified_NGO_User**: A user authenticated by Clerk JWT who has a linked `walletAddress` AND an `NgoApplication` row whose `walletAddress` equals the user's linked wallet address (lowercased) AND whose `status` is `APPROVED`.
- **Allowed_Image_Content_Types**: The set of HTTP content types `image/jpeg`, `image/png`, `image/webp`.
- **Max_Image_Size_Bytes**: The integer constant `5242880` (5 MB, 5 × 1024 × 1024).

## Requirements

### Requirement 1: Cover Image Upload Endpoint

**User Story:** As a verified NGO, I want to upload a cover image for my pool so that the IPFS CID can be embedded in my pool metadata.

#### Acceptance Criteria

1. WHEN an authenticated `POST` request with `Content-Type: multipart/form-data` and exactly one file part named `file` is received at `/api/v1/pools/upload-image`, THE Image_Upload_Endpoint SHALL forward the file's bytes and original filename to Pinata_Pin_File_Url using the Pinata_Headers from the request, and respond with HTTP 200 and a JSON body `{ "cid": <IPFS_CID> }` where the value is the `IpfsHash` field returned by Pinata.
2. THE Image_Upload_Endpoint SHALL accept exactly one file part per request, named `file`.
3. IF the request omits the `file` part or the `file` part has a byte size of 0, THEN THE Image_Upload_Endpoint SHALL return HTTP 400 with error code `IMAGE_FILE_REQUIRED` and SHALL NOT invoke the Pinata_Client.
4. IF the request includes more than one file part, THEN THE Image_Upload_Endpoint SHALL return HTTP 400 with error code `INVALID_REQUEST` and a message stating that exactly one file part is allowed, and SHALL NOT invoke the Pinata_Client.

### Requirement 2: Cover Image Validation

**User Story:** As an NGO, I want the upload endpoint to reject files with the wrong type or size before contacting Pinata, so that I get a fast, clear error.

#### Acceptance Criteria

1. WHEN the `file` part is received, THE Image_Upload_Endpoint SHALL evaluate validation rules in this order: (a) presence and non-zero byte size, (b) Content-Type membership in Allowed_Image_Content_Types, (c) byte size at or below Max_Image_Size_Bytes.
2. IF the `file` part is missing or has a byte size of 0, THEN THE Image_Upload_Endpoint SHALL return HTTP 400 with error code `IMAGE_FILE_REQUIRED` and SHALL NOT invoke the Pinata_Client.
3. IF the `file` part has a `Content-Type` value that is missing or not in Allowed_Image_Content_Types (image/jpeg, image/png, image/webp), THEN THE Image_Upload_Endpoint SHALL return HTTP 400 with error code `IMAGE_TYPE_NOT_ALLOWED` and a message listing the allowed content types, and SHALL NOT invoke the Pinata_Client.
4. IF the `file` part has a byte size greater than Max_Image_Size_Bytes (5,242,880 bytes), THEN THE Image_Upload_Endpoint SHALL return HTTP 400 with error code `IMAGE_TOO_LARGE` and a message stating the maximum allowed size in bytes, and SHALL NOT invoke the Pinata_Client.

### Requirement 3: Pool Metadata Prepare Endpoint

**User Story:** As a verified NGO, I want the backend to upload my pool metadata JSON to IPFS and return the CID, so that I can pass that CID to `deployPool` on-chain.

#### Acceptance Criteria

1. WHEN an authenticated `POST` request with `Content-Type: application/json` is received at `/api/v1/pools/prepare` with a body conforming to the Pool_Metadata_Schema, THE Prepare_Endpoint SHALL produce a Pool_Metadata_Json document from the request body, wrap that document inside a Pinata_Json_Envelope as the value of the `pinataContent` field, upload the envelope to Pinata_Pin_Json_Url using the Pinata_Headers from the request with a 30-second timeout, and respond with HTTP 200 and a JSON body `{ "cid": <IPFS_CID> }` where the value is the non-blank `IpfsHash` field returned by Pinata.
2. THE Pool_Metadata_Json (the document that ends up pinned at the returned IPFS_CID) SHALL contain exactly the fields `title`, `description`, `region`, `targetAmount`, and SHALL include `coverImage` if and only if the request provided a non-null, non-blank `coverImage` value.
3. THE Prepare_Endpoint SHALL preserve the request values for `title`, `description`, `region`, and `coverImage` byte-for-byte (including whitespace inside non-blank strings) when writing them into the Pool_Metadata_Json.
4. THE Prepare_Endpoint SHALL serialize `targetAmount` as a JSON integer (no decimal point, no quotation marks, no leading zeros) so that `IpfsMetadataService.parseAndValidate` reads the same numeric value via `JsonNode.asLong()`.
5. THE Pinata_Json_Envelope SHALL contain exactly one top-level field, `pinataContent`, whose value is the Pool_Metadata_Json. The envelope SHALL NOT modify the byte content of the Pool_Metadata_Json (no reformatting, reordering, or escaping beyond what the value occupies as-is).

### Requirement 4: Pool Metadata Validation Matching IpfsMetadataService

**User Story:** As the platform, I want the prepare endpoint to reject any metadata that the indexer would later mark INVALID, so that no pool is silently dropped after a successful on-chain deployment.

#### Acceptance Criteria

1. THE Prepare_Endpoint SHALL require the request body to include the fields `title`, `description`, `region`, and `targetAmount`.
2. IF the request body is missing the field `title`, `description`, `region`, or `targetAmount` (the field is absent or its JSON value is `null`), THEN THE Prepare_Endpoint SHALL return HTTP 400 with error code `VALIDATION_ERROR` and a message identifying at least one required field that is missing.
3. IF the request body contains `title`, `description`, or `region` with a JSON value that is not a string, or whose string value is blank (empty or contains only whitespace characters as defined by `String.isBlank()`), THEN THE Prepare_Endpoint SHALL return HTTP 400 with error code `VALIDATION_ERROR` and a message identifying the offending field.
4. IF the request body contains `targetAmount` with a JSON value that is not a JSON integer (for example, a string, boolean, decimal number, array, or object), is less than or equal to 0, or exceeds `Long.MAX_VALUE`, THEN THE Prepare_Endpoint SHALL return HTTP 400 with error code `VALIDATION_ERROR` and a message identifying the `targetAmount` field.
5. WHERE the request body contains the optional field `coverImage` with a non-null JSON value, IF that value is not a string or its string value is blank, THEN THE Prepare_Endpoint SHALL return HTTP 400 with error code `VALIDATION_ERROR` and a message identifying the `coverImage` field.
6. WHERE the request body omits the optional field `coverImage` or sets it to JSON `null`, THE Prepare_Endpoint SHALL omit the `coverImage` field from the Pool_Metadata_Json uploaded to Pinata.
7. THE Prepare_Endpoint SHALL perform metadata validation before invoking the Pinata_Client.

### Requirement 5: Authentication and Wallet Linkage

**User Story:** As the platform, I want only authenticated users with a linked wallet to use the preparation endpoints, so that anonymous callers cannot consume Pinata quotas or prepare pools they cannot deploy.

#### Acceptance Criteria

1. IF a request to the Image_Upload_Endpoint or the Prepare_Endpoint omits a valid Clerk JWT in the `Authorization` header (signature, expiry, or issuer fail), THEN THE Pool_Creation_API SHALL return HTTP 401 and SHALL NOT invoke the Pinata_Client.
2. WHEN a request to the Image_Upload_Endpoint or the Prepare_Endpoint carries a valid Clerk JWT for a user whose `walletAddress` is `null` or empty, THE Pool_Creation_API SHALL return HTTP 403 with error code `WALLET_NOT_LINKED` and SHALL NOT invoke the Pinata_Client.
3. WHEN a request reaches the Image_Upload_Endpoint or the Prepare_Endpoint, THE Pool_Creation_API SHALL resolve the authenticated user from the Clerk JWT subject claim using `UserService.getAuthenticatedUserWithWallet` BEFORE running endpoint-specific business logic.
4. IF the JWT subject claim does not resolve to a known user record, THEN THE Pool_Creation_API SHALL return HTTP 401 and SHALL NOT invoke the Pinata_Client.

### Requirement 6: Verified NGO Authorization

**User Story:** As the platform, I want to ensure only verified NGOs prepare pool metadata, so that the prepare flow stays aligned with the on-chain whitelist that `deployPool` will check.

#### Acceptance Criteria

1. WHEN a request to the Image_Upload_Endpoint or the Prepare_Endpoint passes the Authentication and Wallet Linkage checks, THE Pool_Creation_API SHALL normalize the authenticated user's `walletAddress` to lowercase and look up an `NgoApplication` row matching that address and `status = APPROVED` using `NgoApplicationRepository.findByWalletAddressAndStatus`.
2. IF the lookup defined in clause 1 returns no row, THEN THE Pool_Creation_API SHALL return HTTP 403 with error code `NGO_NOT_APPROVED` and an error message indicating that the authenticated wallet has no approved NGO application, without reading the Pinata_Headers, without invoking the Pinata_Client, and without persisting any uploaded image bytes or prepared metadata.
3. THE Pool_Creation_API SHALL perform the verified-NGO check defined in clause 1 before reading the Pinata_Headers and before invoking the Pinata_Client.

### Requirement 7: Pinata API Key Handling

**User Story:** As an NGO, I want the backend to use my Pinata API key only for my upload and never store or log it, so that my key remains under my control.

#### Acceptance Criteria

1. THE Pool_Creation_API SHALL read the NGO's Pinata API key only from Pinata_Api_Key_Header (X-Pinata-Api-Key) and Pinata_Secret_Api_Key_Header (X-Pinata-Secret-Api-Key) on the incoming request, matching header names case-insensitively per HTTP semantics.
2. IF a request that has passed authentication, wallet, and NGO checks omits Pinata_Api_Key_Header or Pinata_Secret_Api_Key_Header, or sends either header with an empty or whitespace-only value, THEN THE Pool_Creation_API SHALL return HTTP 400 with error code `PINATA_KEY_REQUIRED` and a message naming the missing or blank header, and SHALL NOT initiate any outbound request to Pinata.
3. WHEN both Pinata_Api_Key_Header and Pinata_Secret_Api_Key_Header are present with non-empty, non-whitespace-only values on the incoming request, THE Pool_Creation_API SHALL forward those values verbatim as `pinata_api_key` and `pinata_secret_api_key` headers on the outbound request to Pinata.
4. WHEN the outbound HTTP exchange to Pinata terminates, whether by success, error response, network failure, or timeout, THE Pool_Creation_API SHALL discard the Pinata_Api_Key_Header and Pinata_Secret_Api_Key_Header values from memory before returning a response to the caller.
5. THE Pool_Creation_API SHALL NOT persist the values of Pinata_Api_Key_Header or Pinata_Secret_Api_Key_Header to any database table, file, cache, or other storage.
6. THE Pool_Creation_API SHALL NOT include the values of Pinata_Api_Key_Header or Pinata_Secret_Api_Key_Header in any log statement, exception message, or error response body emitted by the application, including responses returned when the outbound Pinata request fails or times out.
7. WHEN a log statement records that a Pinata request was made, THE Pool_Creation_API SHALL log only the target URL, HTTP status code, and (on success) the returned IPFS_CID.

### Requirement 8: Pinata Upstream Error Handling

**User Story:** As an NGO, I want clear error responses when Pinata rejects or fails my upload, so that I can correct the cause and retry.

#### Acceptance Criteria

1. WHEN the Pinata_Client receives a 200 response from Pinata_Pin_File_Url or Pinata_Pin_Json_Url whose body parses as JSON and contains a non-blank string `IpfsHash` field, THE Pool_Creation_API SHALL respond with HTTP 200 and the `cid` field set to that `IpfsHash` value.
2. IF the Pinata_Client receives an HTTP 401 or HTTP 403 response from Pinata, THEN THE Pool_Creation_API SHALL return HTTP 400 with error code `PINATA_UNAUTHORIZED` and a message stating that Pinata rejected the supplied API key.
3. IF the Pinata_Client receives any other HTTP response code from Pinata in the range 400–499 (other than 401 and 403), THEN THE Pool_Creation_API SHALL return HTTP 400 with error code `PINATA_REJECTED_REQUEST` and a message including the upstream numeric status code.
4. IF the Pinata_Client receives an HTTP response code from Pinata in the range 500–599, THEN THE Pool_Creation_API SHALL return HTTP 502 with error code `PINATA_UPSTREAM_ERROR`.
5. IF the Pinata_Client cannot complete the HTTP exchange because of a connection error, DNS error, or a read/connect timeout (configured at 30 seconds), THEN THE Pool_Creation_API SHALL return HTTP 502 with error code `PINATA_UNREACHABLE`.
6. IF the Pinata_Client receives an HTTP 200 response from Pinata whose body cannot be parsed as JSON or whose `IpfsHash` field is missing, non-string, or blank, THEN THE Pool_Creation_API SHALL return HTTP 502 with error code `PINATA_INVALID_RESPONSE`.
7. WHEN the Pool_Creation_API returns any response defined in clauses 2 through 6, THE Pool_Creation_API SHALL NOT persist any uploaded image bytes, prepared metadata document, or Pinata API key values, and SHALL NOT include a `cid` field in the error response body.

### Requirement 9: CID Response Shape

**User Story:** As a frontend developer, I want a stable JSON response shape from both endpoints, so that I can parse the CID without endpoint-specific code.

#### Acceptance Criteria

1. WHEN the Image_Upload_Endpoint returns HTTP 200, THE response body SHALL be a JSON object with exactly one field, `cid`, whose value is a non-blank string.
2. WHEN the Prepare_Endpoint returns HTTP 200, THE response body SHALL be a JSON object with exactly one field, `cid`, whose value is a non-blank string.
3. THE Pool_Creation_API SHALL set the response `Content-Type` header to `application/json` for every response from the Image_Upload_Endpoint and the Prepare_Endpoint, including error responses.
4. THE Pool_Creation_API SHALL emit error responses using the existing `ErrorResponse` record `{ errorCode, message, timestamp }` produced by `GlobalExceptionHandler`.
5. WHEN the Pool_Creation_API returns any non-200 response, THE response body SHALL NOT contain a `cid` field.

### Requirement 10: Round-Trip Compatibility With IpfsMetadataService

**User Story:** As the platform, I want the Pool_Metadata_Json produced by the prepare endpoint to round-trip cleanly through the indexer's reader, so that a successfully prepared pool will always be indexed once `PoolDeployed` fires.

#### Acceptance Criteria

1. WHEN the Prepare_Endpoint accepts a request body and returns HTTP 200, THE resulting Pool_Metadata_Json, when passed to `IpfsMetadataService.parseAndValidate`, SHALL produce a `MetadataResult.Valid`.
2. THE `PoolMetadata` produced from clause 1 SHALL have `title`, `description`, and `region` values that are exact-codepoint matches of the corresponding request fields with no trimming, case folding, or normalization.
3. THE `PoolMetadata` produced from clause 1 SHALL have a `targetAmount` value that equals the request's `targetAmount` exactly, with no rounding, truncation, or scale change.
4. THE `PoolMetadata` produced from clause 1 SHALL have a `coverImage` value equal to the request's `coverImage` string when provided non-null and non-blank, OR `null` when the request omitted `coverImage` or set it to JSON `null`.
5. WHEN the Prepare_Endpoint rejects a request body with HTTP 400 because of a Pool_Metadata_Schema violation, THE same field values arranged in the Pool_Metadata_Json structure (same field names and same JSON types) SHALL produce a `MetadataResult.Invalid` when passed to `IpfsMetadataService.parseAndValidate`.
