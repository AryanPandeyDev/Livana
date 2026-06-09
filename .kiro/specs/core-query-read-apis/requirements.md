# Requirements Document

## Introduction

Build the complete query/read API layer for the Livana backend so the frontend can retrieve and display on-chain indexed data. The event indexer already persists blockchain events to Postgres — this feature adds REST endpoints (controllers, services, DTOs) to serve that data. The scope covers pool browsing, donation queries, proof tracking, NGO reputation, and platform-wide statistics.

## Glossary

- **Pool_API**: The REST controller responsible for serving pool data from the pools table at `/api/v1/pools`
- **Donation_API**: The REST controller responsible for serving donation data from the donations table at `/api/v1/donations`
- **Proof_API**: The REST controller responsible for serving proof data from the proofs table at `/api/v1/proofs`
- **Reputation_API**: The REST controller responsible for serving NGO reputation data aggregated from the sbt_mints table at `/api/v1/reputation`
- **Stats_API**: The REST controller responsible for serving platform-wide aggregate statistics at `/api/v1/stats`
- **Pageable**: Spring Data's pagination abstraction accepting `page`, `size`, and `sort` query parameters
- **USDC_Raw_Amount**: A BIGINT value representing USDC with 6 decimal places (1 USDC = 1000000)
- **Pool_Address**: A lowercase VARCHAR(42) Ethereum address identifying a deployed FundPool contract
- **Donor_Address**: A lowercase VARCHAR(42) Ethereum address identifying a wallet that has made donations
- **NGO_Address**: A lowercase VARCHAR(42) Ethereum address identifying a verified NGO wallet

## Requirements

### Requirement 1: List Pools with Pagination and Filtering

**User Story:** As a donor, I want to browse all indexed pools with pagination and optional region filtering or title search, so that I can discover causes to donate to.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/pools` without filters, THE Pool_API SHALL return a paginated list of pools ordered by `deployed_at` descending
2. WHEN a GET request includes a `region` query parameter, THE Pool_API SHALL return only pools whose region field matches the parameter value exactly (case-insensitive)
3. WHEN a GET request includes a `search` query parameter with at least 1 character, THE Pool_API SHALL return only pools whose title contains the search term as a substring (case-insensitive)
4. WHEN a GET request includes both `region` and `search` parameters, THE Pool_API SHALL return only pools matching both criteria
5. THE Pool_API SHALL accept `page` (zero-based, default 0), `size` (default 20, maximum 100), and `sort` parameters via Spring Pageable for pagination control, using `deployed_at` descending as the default sort when `sort` is not provided
6. THE Pool_API SHALL return each pool with: onChainAddress, title, description, region, coverImageCid, targetAmount, totalDonated, totalReleased, isPaused, deployedAt
7. WHEN the `page` parameter exceeds the available pages, THE Pool_API SHALL return an empty content array with correct pagination metadata
8. IF the `search` parameter is provided but blank (empty or whitespace-only), THEN THE Pool_API SHALL ignore the search filter and return results as if `search` were not provided
9. IF the `size` parameter exceeds 100, THEN THE Pool_API SHALL cap the page size at 100

### Requirement 2: Get Pool Detail

**User Story:** As a donor, I want to view a single pool's full details including aggregated donation and proof data, so that I can make an informed donation decision.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/pools/{address}` with an address that corresponds to an indexed pool, THE Pool_API SHALL return HTTP 200 with the full pool record including metadata and running totals, without requiring authentication
2. THE Pool_API SHALL include in the pool detail response: onChainAddress, creatorAddress, poolIndex, metadataCid, title, description, region, coverImageCid, targetAmount, totalDonated, totalReleased, isPaused, deployTxHash, deployBlock, deployedAt
3. IF a GET request is made to `/api/v1/pools/{address}` with an address that does not correspond to an indexed pool, THEN THE Pool_API SHALL return HTTP 404 with error code `POOL_NOT_FOUND`
4. IF a GET request is made to `/api/v1/pools/{address}` where the address is not a valid 42-character hexadecimal string prefixed with "0x", THEN THE Pool_API SHALL return HTTP 400 with an error message indicating the address format is invalid

### Requirement 3: List Donations by Pool

**User Story:** As a public visitor, I want to see a pool's donation history, so that I can verify it receives real support.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/donations/pool/{poolAddress}`, THE Donation_API SHALL return a paginated list of donations for the specified pool ordered by `block_timestamp` descending, where the response includes standard Spring Page metadata (totalElements, totalPages, current page number, page size)
2. THE Donation_API SHALL return each donation with: donorAddress (lowercase, 42-character hex string), amount (raw BIGINT value representing USDC with 6 decimal places), txHash (66-character hex string), blockTimestamp (ISO-8601 format with timezone offset)
3. THE Donation_API SHALL accept `page` (zero-based, default 0), `size` (default 20, minimum 1, maximum 100), and `sort` parameters via Spring Pageable for pagination control
4. IF the `poolAddress` path parameter is not a valid 42-character lowercase hexadecimal address prefixed with "0x", THEN THE Donation_API SHALL return an HTTP 400 response with an error message indicating invalid pool address format
5. WHEN a GET request is made with a valid `poolAddress` that has no recorded donations, THE Donation_API SHALL return an HTTP 200 response with an empty content array and totalElements equal to 0

### Requirement 4: List Donations by Donor

**User Story:** As a donor, I want to see all my donations across all pools, so that I can track my giving history.

#### Acceptance Criteria

1. WHEN an authenticated GET request is made to `/api/v1/donations/donor/{donorAddress}`, THE Donation_API SHALL return a paginated list of donations for the specified donor ordered by `block_timestamp` descending
2. THE Donation_API SHALL return each donation with: poolAddress, amount (raw USDC value as integer with 6 decimal places), txHash, and blockTimestamp
3. IF an unauthenticated request is made to `/api/v1/donations/donor/{donorAddress}`, THEN THE Donation_API SHALL return HTTP 401
4. THE Donation_API SHALL accept `page`, `size`, and `sort` parameters via Spring Pageable for pagination control with a default page size of 20 and a maximum page size of 100
5. IF the `donorAddress` path parameter is not a valid 42-character hexadecimal Ethereum address, THEN THE Donation_API SHALL return HTTP 400 with an error message indicating an invalid address format
6. WHEN an authenticated GET request is made to `/api/v1/donations/donor/{donorAddress}` and no donations exist for the specified donor, THE Donation_API SHALL return HTTP 200 with an empty page result containing zero elements and a total count of 0

### Requirement 5: Donor Leaderboard

**User Story:** As a public visitor, I want to see a leaderboard of top donors ranked by total donated amount, so that generous giving is publicly recognized.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/donations/leaderboard`, THE Donation_API SHALL return a list of donors ranked by total donated amount in descending order
2. THE Donation_API SHALL compute donor totals by grouping all donations by donor_address and summing the amount field
3. THE Donation_API SHALL return each leaderboard entry with: donorAddress, totalDonated, donationCount
4. THE Donation_API SHALL accept a `limit` query parameter to control the number of results returned, defaulting to 10, with a minimum value of 1 and a maximum value of 100
5. WHEN two donors have the same total donated amount, THE Donation_API SHALL use donor_address in ascending alphabetical order as a secondary sort for deterministic ordering
6. IF the `limit` query parameter is not a valid integer or is outside the range of 1 to 100, THEN THE Donation_API SHALL reject the request with an error response indicating the invalid parameter value
7. WHEN a GET request is made to `/api/v1/donations/leaderboard` and no donations exist, THE Donation_API SHALL return an empty list

### Requirement 6: List Proofs by Pool

**User Story:** As a donor, I want to see all proof submissions for a pool including their release status, so that I can verify how funds are being used.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/proofs/pool/{poolAddress}`, THE Proof_API SHALL return a paginated list of proofs for the specified pool with default ordering by `submitted_at` descending
2. THE Proof_API SHALL return each proof with: proofId (on-chain integer proof ID), ipfsCid, amount (BIGINT, USDC 6 decimals), released (boolean), submittedAt (ISO-8601 timestamp), releasedAt (ISO-8601 timestamp, null if not yet released)
3. THE Proof_API SHALL accept `page`, `size`, and `sort` parameters via Spring Pageable with a default page size of 20 and a maximum page size of 100
4. IF the `poolAddress` path parameter is not a valid 42-character hexadecimal Ethereum address, THEN THE Proof_API SHALL return HTTP 400 with an error message indicating invalid pool address format
5. WHEN a GET request is made to `/api/v1/proofs/pool/{poolAddress}` and no proofs exist for that pool, THE Proof_API SHALL return HTTP 200 with an empty paginated response (empty content array, totalElements 0)

### Requirement 7: List Pending Proofs (Admin)

**User Story:** As an admin, I want to see all pending proof submissions across all pools, so that I know what needs multi-sig review.

#### Acceptance Criteria

1. WHEN an authenticated GET request with ADMIN role is made to `/api/v1/admin/proofs/pending`, THE Proof_API SHALL return a paginated list of proofs where `released` is false, ordered by `submitted_at` ascending
2. THE Proof_API SHALL return each pending proof with the following fields: poolAddress, proofId, ipfsCid, amount (raw USDC value as stored in the database, 6-decimal integer), and submittedAt (ISO-8601 timestamp)
3. IF a request without a valid authentication token is made to `/api/v1/admin/proofs/pending`, THEN THE Proof_API SHALL return HTTP 401
4. IF a request with a valid authentication token but without ADMIN role is made to `/api/v1/admin/proofs/pending`, THEN THE Proof_API SHALL return HTTP 403
5. THE Proof_API SHALL accept `page` (zero-based, default 0), `size` (default 20, maximum 100), and `sort` parameters via Spring Pageable for pagination control
6. WHEN no pending proofs exist, THE Proof_API SHALL return an empty page with totalElements equal to 0 and an empty content array
7. THE Proof_API SHALL enforce the ADMIN role check via `@PreAuthorize("hasRole('ADMIN')")`

### Requirement 8: Get NGO Reputation

**User Story:** As a donor, I want to see an NGO's reputation score including total SBTs and funds released, so that I can assess their trustworthiness.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/reputation/{ngoAddress}`, THE Reputation_API SHALL return the aggregated reputation data containing ngoAddress, totalSbts, totalAmountReleased (BIGINT, raw USDC with 6 decimals), and poolCount
2. THE Reputation_API SHALL compute reputation by aggregating all sbt_mints records matching the specified NGO address: totalSbts as the count of records, totalAmountReleased as the sum of the amount column, and poolCount as the count of distinct pool_address values
3. WHEN a GET request is made with an NGO address that has no SBT mints, THE Reputation_API SHALL return a 200 response with zero values (totalSbts: 0, totalAmountReleased: 0, poolCount: 0)
4. IF the ngoAddress path parameter is not a valid Ethereum address (42-character lowercase hex string starting with "0x"), THEN THE Reputation_API SHALL return a 400 error response indicating an invalid address format
5. THE Reputation_API SHALL be publicly accessible without authentication

### Requirement 9: NGO Reputation Leaderboard

**User Story:** As a public visitor, I want to see a leaderboard of top-rated NGOs by reputation, so that I can discover trustworthy organizations.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/reputation/leaderboard`, THE Reputation_API SHALL return a list of NGOs ranked by total released amount in descending order
2. THE Reputation_API SHALL compute each entry by grouping sbt_mints by ngo_address: SUM(amount) for total released, COUNT(*) for total SBTs, COUNT(DISTINCT pool_address) for pool count
3. THE Reputation_API SHALL return each leaderboard entry with: ngoAddress, totalSbts, totalAmountReleased, poolCount, and rank (1-based position in the leaderboard)
4. THE Reputation_API SHALL accept a `limit` query parameter to control the number of results returned, defaulting to 10, with a minimum of 1 and a maximum of 100
5. WHEN two NGOs have the same total released amount, THE Reputation_API SHALL use ngo_address in ascending lexicographic order as a tiebreaker for deterministic ordering
6. IF the `limit` query parameter is not a positive integer or exceeds 100, THEN THE Reputation_API SHALL return a 400 status code with an error message indicating the valid range (1 to 100)
7. WHEN no sbt_mints records exist in the system, THE Reputation_API SHALL return an empty array with a 200 status code

### Requirement 10: Platform Statistics

**User Story:** As a public visitor, I want to see platform-wide statistics, so that I can gauge the platform's overall impact.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/v1/stats`, THE Stats_API SHALL return an HTTP 200 response containing the fields: totalDonated, totalReleased, activePoolsCount, and verifiedNgosCount
2. THE Stats_API SHALL compute totalDonated as the sum of `total_donated` across all pools regardless of pause state
3. THE Stats_API SHALL compute totalReleased as the sum of `total_released` across all pools regardless of pause state
4. THE Stats_API SHALL compute activePoolsCount as the count of pools where `is_paused` is false
5. THE Stats_API SHALL compute verifiedNgosCount as the count of distinct `creator_address` values across all pools regardless of pause state
6. THE Stats_API SHALL NOT require authentication to access the `/api/v1/stats` endpoint
7. IF no pools exist in the system, THEN THE Stats_API SHALL return totalDonated as 0, totalReleased as 0, activePoolsCount as 0, and verifiedNgosCount as 0
8. THE Stats_API SHALL return the response within 2000 milliseconds under normal operating conditions

### Requirement 11: Error Handling for Invalid Addresses

**User Story:** As a developer integrating with the API, I want clear error responses when I provide malformed addresses, so that I can debug my integration.

#### Acceptance Criteria

1. WHEN a request is made to any endpoint with an address parameter that contains a valid 42-character hex string starting with `0x` but uses mixed-case (EIP-55 checksummed) characters, THE system SHALL accept the address by normalizing it to lowercase before processing
2. WHEN a request is made to any endpoint with an address parameter that is not a valid 42-character hex string starting with `0x` (e.g., wrong length, non-hex characters, or missing `0x` prefix), THE system SHALL return HTTP 400 with error code `INVALID_ADDRESS` and a message indicating which validation rule the address violated
3. THE system SHALL validate address format before querying the database
4. IF the address parameter is an empty string or null where an address is required, THEN THE system SHALL return HTTP 400 with error code `INVALID_ADDRESS`

### Requirement 12: DTO Response Layer

**User Story:** As a developer, I want API responses to use dedicated DTOs that exclude internal fields, so that the API contract is stable and decoupled from entity changes.

#### Acceptance Criteria

1. THE system SHALL use dedicated response DTO Java records for all API responses across the pool, donation, proof, and reputation modules, separate from JPA entity classes
2. THE system SHALL exclude the following internal fields from public API responses: the auto-generated UUID primary key (`id`), `indexedAt`, `blockNumber`, `logIndex`, `submittedBlock`, and `releasedBlock`
3. THE system SHALL represent all USDC monetary amounts (`amount`, `targetAmount`, `totalDonated`, `totalReleased`) as long values encoding the raw 6-decimal BIGINT (i.e., 1 USDC = 1000000) in API responses
4. THE system SHALL expose on-chain identifiers (`onChainAddress`, `poolAddress`, `txHash`, `donorAddress`) in response DTOs as the public-facing resource identifiers instead of internal UUID primary keys
