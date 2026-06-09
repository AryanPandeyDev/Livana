# Implementation Plan: Pool Creation Preparation

## Overview

Implement the two backend endpoints (`POST /api/v1/pools/upload-image` and `POST /api/v1/pools/prepare`) that a verified NGO uses before calling `deployPool()` on-chain. The implementation forwards uploads to Pinata using request-scoped headers (never persisted, never logged), produces canonical metadata JSON that round-trips cleanly through the existing `IpfsMetadataService`, and reuses the existing `ApiException` / `GlobalExceptionHandler` infrastructure for all error responses. No persistence, no on-chain calls, no new database migrations.

The implementation order follows the dependency chain in the design: foundation (config + multipart limits) → exception types → value types and DTOs → stateless components → services → controller → tests.

## Tasks

- [x] 1. Foundation layer (multipart config, framework error handler, HttpClient bean)
  - [x] 1.1 Configure multipart limits in application.properties
    - Edit `backend/src/main/resources/application.properties`
    - Add `spring.servlet.multipart.max-file-size=5MB` and `spring.servlet.multipart.max-request-size=5MB`
    - Value MUST align with `Max_Image_Size_Bytes = 5,242,880` enforced by `ImageValidator`
    - _Requirements: 2.4_

  - [x] 1.2 Add MaxUploadSizeExceededException handler to GlobalExceptionHandler
    - Edit `backend/src/main/java/com/livana/backend/common/exception/GlobalExceptionHandler.java`
    - Add `@ExceptionHandler(MaxUploadSizeExceededException.class)` method that returns HTTP 400 with `ErrorResponse(errorCode="IMAGE_TOO_LARGE", message="Image exceeds maximum allowed size of 5242880 bytes", timestamp=now)`
    - Do not expose the framework exception message to the client
    - _Requirements: 2.4_

  - [x] 1.3 Create PinataHttpConfig bean exposing java.net.http.HttpClient
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/config/PinataHttpConfig.java`
    - `@Configuration` class with `@Bean HttpClient pinataHttpClient()` returning `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()`
    - Bean is injected into `PinataClient` so tests can swap it for a fake
    - _Requirements: 8.5_

- [ ] 2. Exception classes (one file per error code)
  - [x] 2.1 Create NgoNotApprovedException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/NgoNotApprovedException.java`
    - Subclass of `ApiException` with `HttpStatus.FORBIDDEN` and error code `NGO_NOT_APPROVED`
    - Message: "Authenticated wallet has no approved NGO application"
    - _Requirements: 6.2_

  - [x] 2.2 Create PinataKeyRequiredException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataKeyRequiredException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `PINATA_KEY_REQUIRED`
    - Constructor takes the missing/blank header name and includes it in the message; do not include any header value
    - _Requirements: 7.2_

  - [x] 2.3 Create ImageFileRequiredException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/ImageFileRequiredException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `IMAGE_FILE_REQUIRED`
    - Message: "A non-empty file part named 'file' is required"
    - _Requirements: 1.3, 2.2_

  - [x] 2.4 Create ImageTypeNotAllowedException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/ImageTypeNotAllowedException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `IMAGE_TYPE_NOT_ALLOWED`
    - Message lists the allowed content types (`image/jpeg`, `image/png`, `image/webp`)
    - _Requirements: 2.3_

  - [x] 2.5 Create ImageTooLargeException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/ImageTooLargeException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `IMAGE_TOO_LARGE`
    - Constructor takes max bytes; message states "Image exceeds maximum allowed size of <maxBytes> bytes"
    - _Requirements: 2.4_

  - [x] 2.6 Create PinataUnauthorizedException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataUnauthorizedException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `PINATA_UNAUTHORIZED`
    - Message: "Pinata rejected the supplied API key"
    - _Requirements: 8.2_

  - [x] 2.7 Create PinataRejectedException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataRejectedException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_REQUEST` and error code `PINATA_REJECTED_REQUEST`
    - Constructor takes upstream `int status` and includes it in the message; expose the status via a getter for testing/logging
    - _Requirements: 8.3_

  - [x] 2.8 Create PinataUpstreamException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataUpstreamException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_GATEWAY` and error code `PINATA_UPSTREAM_ERROR`
    - Message: "Pinata returned an upstream error"
    - _Requirements: 8.4_

  - [x] 2.9 Create PinataUnreachableException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataUnreachableException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_GATEWAY` and error code `PINATA_UNREACHABLE`
    - Constructor accepts a generic reason string (timeout / connection failure); never include header values
    - _Requirements: 8.5_

  - [x] 2.10 Create PinataInvalidResponseException
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/exception/PinataInvalidResponseException.java`
    - Subclass of `ApiException` with `HttpStatus.BAD_GATEWAY` and error code `PINATA_INVALID_RESPONSE`
    - Constructor accepts a reason string ("Missing or blank IpfsHash" / "Malformed Pinata response")
    - _Requirements: 8.6_

- [x] 3. Value types and DTOs
  - [x] 3.1 Create PinataHeaders value type
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/PinataHeaders.java`
    - `final` class with `char[] apiKey` and `char[] secretKey` fields and constants `API_KEY_HEADER = "X-Pinata-Api-Key"`, `SECRET_KEY_HEADER = "X-Pinata-Secret-Api-Key"`
    - Static factory `fromRequest(HttpServletRequest request)` reads both headers (case-insensitive via `request.getHeader`), throws `PinataKeyRequiredException(headerName)` when either is null/blank, returns a populated instance
    - Methods `apiKey()` and `secretKey()` materialize fresh `String` instances from the `char[]` for outbound header construction
    - Method `clear()` overwrites both `char[]` fields with `'\0'` and nulls them out; safe to call multiple times
    - _Requirements: 7.1, 7.2, 7.4_

  - [x] 3.2 Create PreparePoolRequest record
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/dto/PreparePoolRequest.java`
    - Java record `(String title, String description, String region, String coverImage, long targetAmount)`
    - No Bean Validation annotations; `PoolMetadataValidator` is the single source of truth
    - `coverImage` is nullable
    - _Requirements: 3.2, 4.1_

  - [x] 3.3 Create CidResponse record
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/dto/CidResponse.java`
    - Java record `(String cid)` — single field, nothing else
    - Used as the success body for both endpoints
    - _Requirements: 9.1, 9.2_

- [x] 4. Stateless components (validators, JSON builder)
  - [x] 4.1 Create ImageValidator
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/ImageValidator.java`
    - `final` class with private constructor (utility class)
    - Constants `MAX_BYTES = 5L * 1024 * 1024` and `ALLOWED = Set.of("image/jpeg", "image/png", "image/webp")`
    - Static method `validate(MultipartFile file)` evaluates rules in order: presence (null or `isEmpty()`) → content-type (lowercased, in `ALLOWED`) → size (`getSize() <= MAX_BYTES`)
    - Throws `ImageFileRequiredException`, `ImageTypeNotAllowedException`, or `ImageTooLargeException` at the first failing rule
    - _Requirements: 1.3, 2.1, 2.2, 2.3, 2.4_

  - [x] 4.2 Create PoolMetadataValidator
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/PoolMetadataValidator.java`
    - `@Component` class with public method `validate(JsonNode body)` returning `PreparePoolRequest`
    - Reject non-object bodies with `ApiException(BAD_REQUEST, "VALIDATION_ERROR", ...)`
    - For `title`, `description`, `region`: require present, non-null, textual, non-blank (matches `IpfsMetadataService` rules)
    - For `targetAmount`: require present, non-null, integral number, `canConvertToLong()`, value > 0
    - For `coverImage`: optional; when present and non-null, must be textual and non-blank; when absent or JSON null, return `null`
    - All rejections throw `ApiException(BAD_REQUEST, "VALIDATION_ERROR", "<field> ...")` identifying the offending field
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 4.3 Create PoolMetadataJsonBuilder
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/PoolMetadataJsonBuilder.java`
    - `@Component` class with `ObjectMapper` injected via constructor (`@RequiredArgsConstructor`)
    - Public method `toCanonicalJson(PreparePoolRequest req)` returns `byte[]`
    - Build an `ObjectNode` with fields written in fixed order: `title`, `description`, `region`, optional `coverImage` (only when non-null), `targetAmount`
    - Use `ObjectNode.put(String, long)` for `targetAmount` to produce a JSON integer literal (no quotes, no decimal)
    - Serialize via `objectMapper.writeValueAsBytes(root)`; wrap any `JsonProcessingException` in `IllegalStateException` ("Unreachable: ObjectNode is always serializable")
    - _Requirements: 3.2, 3.3, 3.4, 4.6_

- [x] 5. Services (auth + Pinata HTTP wrapper)
  - [x] 5.1 Create NgoAuthorizationService
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/NgoAuthorizationService.java`
    - `@Service` class with `UserService` and `NgoApplicationRepository` injected
    - Public method `requireApprovedNgo(Jwt jwt)` returns `User`
    - Step 1: resolve user via `userService.getAuthenticatedUserWithWallet(jwt.getSubject())`; convert any thrown `USER_NOT_FOUND` `ApiException` to `ApiException(UNAUTHORIZED, "USER_NOT_FOUND", ...)` so JWT-subject-not-found surfaces as HTTP 401 (Requirement 5.4)
    - Step 2: lowercase the user's wallet address with `Locale.ROOT` (defensive, even though `UserService.linkWallet` already lowercases)
    - Step 3: call `ngoApplicationRepository.findByWalletAddressAndStatus(lowercased, ApplicationStatus.APPROVED)`; throw `NgoNotApprovedException` when empty
    - Method MUST run all three checks in the documented order before any caller reads Pinata headers or invokes Pinata
    - _Requirements: 5.3, 5.4, 6.1, 6.2, 6.3_

  - [x] 5.2 Create PinataClient
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/service/PinataClient.java`
    - `@Service` class with `HttpClient` (from `PinataHttpConfig`) and `ObjectMapper` injected
    - Constants `PIN_FILE_URL = URI.create("https://api.pinata.cloud/pinning/pinFileToIPFS")`, `PIN_JSON_URL = URI.create("https://api.pinata.cloud/pinning/pinJSONToIPFS")`, `TIMEOUT = Duration.ofSeconds(30)`
    - Public methods `pinFile(PinataHeaders keys, MultipartFile file) -> String` and `pinJson(PinataHeaders keys, byte[] canonicalJson) -> String`
    - Build outbound `HttpRequest` with per-request `.timeout(TIMEOUT)`, header `pinata_api_key` = `keys.apiKey()`, header `pinata_secret_api_key` = `keys.secretKey()`
    - Build the file multipart body manually (RFC 7578: `--boundary\r\nContent-Disposition: form-data; name="file"; filename="<original>"\r\nContent-Type: <mime>\r\n\r\n<bytes>\r\n--boundary--\r\n`); use `UUID`-suffixed boundary; `Content-Type: multipart/form-data; boundary=<boundary>`
    - Build the JSON request with `Content-Type: application/json` and `BodyPublishers.ofByteArray(canonicalJson)`
    - Send via `httpClient.send(request, HttpResponse.BodyHandlers.ofString())`; on `HttpTimeoutException` throw `PinataUnreachableException("Pinata request timed out")`; on `IOException` or `InterruptedException` (re-set interrupt flag) throw `PinataUnreachableException("Pinata request failed")`
    - Map status codes: `200` → extract `IpfsHash` (textual, non-blank) or throw `PinataInvalidResponseException`; `401`/`403` → `PinataUnauthorizedException`; `[400,500)` other → `PinataRejectedException(status)`; `[500,600)` → `PinataUpstreamException`; otherwise `PinataInvalidResponseException("Unexpected status " + status)`
    - Log only URL, status code, and (on success) the CID; never log header values or response bodies
    - _Requirements: 7.3, 7.6, 7.7, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

- [x] 6. Controller
  - [x] 6.1 Create PoolPreparationController
    - Create `backend/src/main/java/com/livana/backend/pool/preparation/controller/PoolPreparationController.java`
    - `@RestController @RequestMapping("/api/v1/pools") @RequiredArgsConstructor`
    - Inject `NgoAuthorizationService`, `PinataClient`, `PoolMetadataValidator`, `PoolMetadataJsonBuilder`
    - Endpoint `POST /upload-image` (`consumes = MULTIPART_FORM_DATA_VALUE`): take `@AuthenticationPrincipal Jwt jwt`, `@RequestPart(name="file", required=false) MultipartFile file`, and `HttpServletRequest request`; call order is `requireApprovedNgo(jwt)` → multi-file-part check via `MultipartHttpServletRequest.getFileMap()` (more than one part → `ApiException(BAD_REQUEST, "INVALID_REQUEST", "exactly one file part is allowed")`) → `ImageValidator.validate(file)` → `PinataHeaders.fromRequest(request)` → `try { pinataClient.pinFile(keys, file) } finally { keys.clear(); }` → return `ResponseEntity.ok(new CidResponse(cid))`
    - Endpoint `POST /prepare` (`consumes = APPLICATION_JSON_VALUE`): take `@AuthenticationPrincipal Jwt jwt`, `@RequestBody JsonNode body`, and `HttpServletRequest request`; call order is `requireApprovedNgo(jwt)` → `metadataValidator.validate(body)` → `metadataJsonBuilder.toCanonicalJson(validated)` → `PinataHeaders.fromRequest(request)` → `try { pinataClient.pinJson(keys, canonicalJson) } finally { keys.clear(); }` → return `ResponseEntity.ok(new CidResponse(cid))`
    - Both endpoints MUST run authorization before reading Pinata headers, before reading Pinata, and before any persistence
    - _Requirements: 1.1, 1.2, 1.4, 3.1, 5.1, 5.2, 6.3, 7.4, 7.5, 9.1, 9.2, 9.3_

- [x] 7. Compile checkpoint
  - Run `./mvnw compile` from `backend/`; ensure the project compiles with no errors before moving on
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Property-based tests (jqwik)
  - [ ]* 8.1 Property test for round-trip with IpfsMetadataService
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolMetadataRoundTripPropertyTest.java`
    - **Property 1: Round-trip with IpfsMetadataService**
    - Generate valid `PreparePoolRequest` (non-blank Unicode strings; positive long up to `Long.MAX_VALUE`; `coverImage` nullable); assert `IpfsMetadataService.parseAndValidate(toCanonicalJson(req))` returns `MetadataResult.Valid` with codepoint-exact field equality
    - Use `@Property(tries = 200)` and the `Arbitraries` providers documented in the design
    - **Validates: Requirements 3.3, 3.4, 10.1, 10.2, 10.3, 10.4**

  - [ ]* 8.2 Property test for validator parity with IpfsMetadataService
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolMetadataValidatorParityPropertyTest.java`
    - **Property 2: Validator parity with IpfsMetadataService**
    - Generate `JsonNode` bodies that the validator rejects (missing/null required fields; non-string or blank `title`/`description`/`region`; non-integer/≤0/out-of-range `targetAmount`; present-but-blank `coverImage`); assert `IpfsMetadataService.parseAndValidate(json)` returns `MetadataResult.Invalid` for the same JSON
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 10.5**

  - [ ]* 8.3 Property test for outbound JSON shape
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolMetadataJsonShapePropertyTest.java`
    - **Property 3: Outbound JSON shape**
    - Generate valid `PreparePoolRequest`; parse the bytes from `toCanonicalJson` as JSON; assert field set equals `{title, description, region, targetAmount}` when `coverImage` is null and `{title, description, region, coverImage, targetAmount}` when non-null
    - **Validates: Requirements 3.2, 4.6**

  - [ ]* 8.4 Property test for image validator decision and error ordering
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/ImageValidatorPropertyTest.java`
    - **Property 4: Image validator decision and error ordering**
    - Generate `(presence, contentType, size)` triples using a synthetic `MultipartFile` (no allocated bytes); assert acceptance iff all three rules pass; assert error code matches the first failing rule in order presence → type → size
    - **Validates: Requirements 1.3, 2.1, 2.2, 2.3, 2.4**

  - [ ]* 8.5 Property test for Pinata header extraction
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PinataHeadersExtractionPropertyTest.java`
    - **Property 5: Pinata header extraction**
    - Generate header value pairs (each absent / null / blank / non-blank) and case-permutations of the header names; build a stub `HttpServletRequest` (e.g. `MockHttpServletRequest`); assert `PinataHeaders.fromRequest` succeeds iff both headers are present with non-blank values, otherwise throws `PinataKeyRequiredException`
    - **Validates: Requirements 7.1, 7.2**

  - [ ]* 8.6 Property test for Pinata header forwarding
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PinataHeadersForwardingPropertyTest.java`
    - **Property 6: Pinata header forwarding**
    - Use a fake `HttpClient` that captures every outbound `HttpRequest`; for arbitrary non-blank `(apiKey, secretKey)` pairs and both `pinFile` / `pinJson` paths, assert the captured request carries `pinata_api_key` and `pinata_secret_api_key` byte-for-byte equal to the inputs
    - **Validates: Requirements 7.3**

  - [ ]* 8.7 Property test for Pinata key non-leakage
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PinataKeyNonLeakagePropertyTest.java`
    - **Property 7: Pinata key non-leakage**
    - Capture log output (e.g. via `ListAppender` on the package logger) and the HTTP response body across every terminal outcome (200; 4xx/5xx; timeout; IO; malformed body; pre-Pinata validation failures; auth failures); for arbitrary non-blank key pairs assert neither the captured logs nor the response body contains the key strings
    - **Validates: Requirements 7.4, 7.6, 7.7**

  - [ ]* 8.8 Property test for Pinata status mapping
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PinataStatusMappingPropertyTest.java`
    - **Property 8: Pinata status mapping**
    - Use a fake `HttpClient` returning canned `(status, body)` pairs; for `status ∈ [200, 600)` and bodies covering valid/malformed/missing-IpfsHash/blank, assert the resulting exception (or success) matches the documented mapping (200+valid → CID; 200+bad → `PINATA_INVALID_RESPONSE`; 401/403 → `PINATA_UNAUTHORIZED`; other 4xx → `PINATA_REJECTED_REQUEST` containing the status; 5xx → `PINATA_UPSTREAM_ERROR`)
    - **Validates: Requirements 8.1, 8.3, 8.4, 8.6**

  - [ ]* 8.9 Property test for error response shape
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/ErrorResponseShapePropertyTest.java`
    - **Property 9: Error response shape**
    - Drive the controller via MockMvc with arbitrary inputs that produce non-200 responses; assert `Content-Type: application/json`, body parses as `ErrorResponse(errorCode, message, timestamp)` with all three fields non-null and no `cid` field present
    - **Validates: Requirements 9.3, 9.4, 9.5, 8.7**

  - [ ]* 8.10 Property test for wallet lowercasing in NGO lookup
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/NgoAuthorizationLowercasingPropertyTest.java`
    - **Property 10: Wallet lowercasing in NGO lookup**
    - Generate 42-char hex addresses with arbitrary case; capture the address argument passed to a mocked `NgoApplicationRepository.findByWalletAddressAndStatus`; assert the captured argument equals `input.toLowerCase(Locale.ROOT)` and the status equals `ApplicationStatus.APPROVED`
    - **Validates: Requirements 6.1**

- [ ] 9. Web-layer integration tests (MockMvc + fake HttpClient)
  - [ ]* 9.1 Integration tests for happy paths
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolPreparationControllerHappyPathTest.java`
    - Use `@WebMvcTest(PoolPreparationController.class)` plus a `@TestConfiguration` that registers a fake `HttpClient` bean returning canned 200 responses with valid `IpfsHash`
    - Cover: `/upload-image` 200 with `{cid}` body; `/prepare` 200 with `{cid}` body for both `coverImage`-present and `coverImage`-absent inputs
    - Assert the response `Content-Type` is `application/json`
    - _Requirements: 1.1, 3.1, 9.1, 9.2, 9.3_

  - [ ]* 9.2 Integration tests for auth, wallet, and NGO authorization paths
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolPreparationControllerAuthTest.java`
    - Cover: missing/invalid JWT → 401; JWT subject not resolvable → 401 `USER_NOT_FOUND`; user without wallet → 403 `WALLET_NOT_LINKED`; user with wallet but no APPROVED `NgoApplication` → 403 `NGO_NOT_APPROVED`
    - Assert ordering: malformed body + missing wallet returns `WALLET_NOT_LINKED` (auth wins); missing Pinata header + no approved NGO returns `NGO_NOT_APPROVED` (NGO check wins); fake `HttpClient` is never invoked on any of these paths
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.2, 6.3_

  - [ ]* 9.3 Integration tests for image validation paths
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolPreparationControllerImageValidationTest.java`
    - Cover: missing `file` part → 400 `IMAGE_FILE_REQUIRED`; empty file → 400 `IMAGE_FILE_REQUIRED`; disallowed content-type (e.g. `application/pdf`) → 400 `IMAGE_TYPE_NOT_ALLOWED`; size > 5 MB enforced by `ImageValidator` → 400 `IMAGE_TOO_LARGE`; multipart framework rejection (size > `max-file-size`) → 400 `IMAGE_TOO_LARGE` via `MaxUploadSizeExceededException` handler; more than one file part → 400 `INVALID_REQUEST`
    - Assert fake `HttpClient` is never invoked on any of these paths
    - _Requirements: 1.3, 1.4, 2.1, 2.2, 2.3, 2.4_

  - [ ]* 9.4 Integration tests for metadata validation paths
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolPreparationControllerMetadataValidationTest.java`
    - Cover: missing required field; field set to JSON null; non-string `title`/`description`/`region`; blank string fields; `targetAmount` as string / decimal / boolean / 0 / negative; `coverImage` present but blank → all return 400 `VALIDATION_ERROR`
    - Assert fake `HttpClient` is never invoked on any of these paths
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7_

  - [ ]* 9.5 Integration tests for Pinata error mapping paths
    - Create `backend/src/test/java/com/livana/backend/pool/preparation/PoolPreparationControllerPinataErrorTest.java`
    - Configure the fake `HttpClient` to return / throw the relevant outcomes; cover: 401/403 → 400 `PINATA_UNAUTHORIZED`; arbitrary 4xx (e.g. 422) → 400 `PINATA_REJECTED_REQUEST` (message contains `422`); 5xx → 502 `PINATA_UPSTREAM_ERROR`; `HttpTimeoutException` → 502 `PINATA_UNREACHABLE`; `IOException` → 502 `PINATA_UNREACHABLE`; 200 with malformed body → 502 `PINATA_INVALID_RESPONSE`; 200 with missing/blank `IpfsHash` → 502 `PINATA_INVALID_RESPONSE`
    - Assert no error response body contains a `cid` field
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

- [x] 10. Final test checkpoint
  - Run `./mvnw test` from `backend/`; ensure all unit, property, and integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP; core implementation tasks are never marked optional
- Each task references specific requirements for traceability
- Property tests use jqwik 1.9.1 (already in `pom.xml`); integration tests use Spring Boot's existing `@WebMvcTest` slice and a fake `HttpClient` bean — no WireMock and no new HTTP client dependency
- The 5 MB multipart limit in `application.properties` MUST stay in sync with `ImageValidator.MAX_BYTES`
- The Pinata `HttpClient` is a Spring bean (from `PinataHttpConfig`) so tests can swap it via `@TestConfiguration` — same seam used by `IpfsMetadataService`
- Logging discipline: `PinataClient` logs only URL, status code, and CID. `PinataHeaders.clear()` runs in the controller's `finally` block to overwrite the `char[]` key material
- Validation parity with `IpfsMetadataService` is the most important invariant in this feature — Properties 1, 2 are the regression net for "metadata silently dropped after on-chain deploy"

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10", "3.2", "3.3"] },
    { "id": 1, "tasks": ["3.1", "4.1", "4.2", "4.3", "5.1"] },
    { "id": 2, "tasks": ["5.2"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.10", "9.1", "9.2", "9.3", "9.4", "9.5"] }
  ]
}
```
