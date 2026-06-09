# Implementation Plan: Core Query / Read APIs

## Overview

Implement the read-only REST API layer for Livana backend. This adds controllers, services, DTOs, Spring Data projections, custom repository queries, and an `AddressValidator` utility to serve indexed on-chain data (pools, donations, proofs, SBT mints) to the frontend. All code is Java 17 on Spring Boot 4.x.

## Tasks

- [x] 1. Add jqwik dependency and create AddressValidator utility
  - [x] 1.1 Add jqwik test dependency to pom.xml
    - Add `net.jqwik:jqwik:1.9.1` with `<scope>test</scope>` to the `<dependencies>` section
    - _Requirements: Testing Strategy from design_

  - [x] 1.2 Create AddressValidator utility class
    - Create `backend/src/main/java/com/livana/backend/common/validation/AddressValidator.java`
    - Implement static method `validateAndNormalize(String address, String paramName)` that validates 42-char hex with `0x` prefix and returns lowercase
    - Throw `ApiException(BAD_REQUEST, "INVALID_ADDRESS", ...)` for null, empty, wrong length, non-hex, or missing prefix
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ]* 1.3 Write property test for AddressValidator — valid addresses (Property 1)
    - **Property 1: Address validation accepts all valid addresses and normalizes to lowercase**
    - Generate arbitrary 40-char hex strings, prepend `0x`, pass through `validateAndNormalize`, assert result equals `input.toLowerCase()`
    - **Validates: Requirements 11.1**

  - [ ]* 1.4 Write property test for AddressValidator — invalid addresses (Property 2)
    - **Property 2: Address validation rejects all invalid addresses**
    - Generate strings that don't match `^0x[0-9a-fA-F]{40}$` (wrong length, non-hex chars, missing prefix, null, empty), assert `ApiException` with 400 status and `INVALID_ADDRESS` code
    - **Validates: Requirements 11.2, 11.4, 2.4, 3.4, 4.5, 6.4, 8.4**

- [x] 2. Create DTOs and Spring Data projections
  - [x] 2.1 Create Pool module DTOs
    - Create `backend/src/main/java/com/livana/backend/pool/dto/PoolSummaryDto.java` (Java record)
    - Create `backend/src/main/java/com/livana/backend/pool/dto/PoolDetailDto.java` (Java record)
    - Fields as specified in design: exclude `id`, `indexedAt`; expose on-chain identifiers
    - _Requirements: 1.6, 2.2, 12.1, 12.2, 12.3, 12.4_

  - [x] 2.2 Create Donation module DTOs
    - Create `backend/src/main/java/com/livana/backend/donation/dto/PoolDonationDto.java`
    - Create `backend/src/main/java/com/livana/backend/donation/dto/DonorDonationDto.java`
    - Create `backend/src/main/java/com/livana/backend/donation/dto/LeaderboardEntryDto.java`
    - _Requirements: 3.2, 4.2, 5.3, 12.1, 12.2, 12.3, 12.4_

  - [x] 2.3 Create Proof module DTOs
    - Create `backend/src/main/java/com/livana/backend/proof/dto/ProofDto.java`
    - Create `backend/src/main/java/com/livana/backend/proof/dto/PendingProofDto.java`
    - _Requirements: 6.2, 7.2, 12.1, 12.2_

  - [x] 2.4 Create Reputation module DTOs
    - Create `backend/src/main/java/com/livana/backend/reputation/dto/NgoReputationDto.java`
    - Create `backend/src/main/java/com/livana/backend/reputation/dto/NgoLeaderboardEntryDto.java`
    - _Requirements: 8.1, 9.3, 12.1_

  - [x] 2.5 Create Stats module DTO
    - Create `backend/src/main/java/com/livana/backend/pool/dto/PlatformStatsDto.java`
    - _Requirements: 10.1, 12.1_

  - [x] 2.6 Create Spring Data projection interfaces
    - Create `backend/src/main/java/com/livana/backend/donation/repository/LeaderboardProjection.java`
    - Create `backend/src/main/java/com/livana/backend/reputation/repository/NgoLeaderboardProjection.java`
    - Create `backend/src/main/java/com/livana/backend/reputation/repository/NgoReputationProjection.java`
    - Create `backend/src/main/java/com/livana/backend/pool/repository/PlatformStatsProjection.java`
    - _Requirements: 5.2, 8.2, 9.2, 10.2, 10.3, 10.4, 10.5_

- [x] 3. Add custom repository query methods
  - [x] 3.1 Add custom queries to PoolRepository
    - Add `findPlatformStats()` returning `PlatformStatsProjection`
    - Add `findByRegionIgnoreCase(String region, Pageable pageable)` returning `Page<Pool>`
    - Add `findByTitleContainingIgnoreCase(String search, Pageable pageable)` returning `Page<Pool>`
    - Add `findByRegionAndTitleContaining(String region, String search, Pageable pageable)` returning `Page<Pool>`
    - _Requirements: 1.2, 1.3, 1.4, 10.2, 10.3, 10.4, 10.5_

  - [x] 3.2 Add custom queries to DonationRepository
    - Add `findDonorLeaderboard(Pageable pageable)` returning `List<LeaderboardProjection>`
    - Uses JPQL GROUP BY `donorAddress`, SUM(amount), COUNT, ORDER BY totalDonated DESC, donorAddress ASC
    - _Requirements: 5.1, 5.2, 5.5_

  - [x] 3.3 Add custom queries to SbtMintRepository
    - Add `findNgoLeaderboard(Pageable pageable)` returning `List<NgoLeaderboardProjection>`
    - Add `findReputationByNgoAddress(String ngoAddress)` returning `NgoReputationProjection`
    - _Requirements: 8.2, 9.2_

- [x] 4. Checkpoint - Compile check
  - Ensure all code compiles with `./mvnw compile`, ask the user if questions arise.

- [x] 5. Implement service layer
  - [x] 5.1 Implement PoolQueryService
    - Create `backend/src/main/java/com/livana/backend/pool/service/PoolQueryService.java`
    - Methods: `listPools(region, search, pageable)`, `getPool(address)`
    - Cap page size at 100, apply region/search filter logic conjunctively
    - Ignore blank search param (trim and treat as null)
    - Throw `ApiException(NOT_FOUND, "POOL_NOT_FOUND", ...)` for missing pool
    - Map entity → DTO inline
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.1, 2.2, 2.3_

  - [x] 5.2 Implement DonationQueryService
    - Create `backend/src/main/java/com/livana/backend/donation/service/DonationQueryService.java`
    - Methods: `donationsByPool(poolAddress, pageable)`, `donationsByDonor(donorAddress, pageable)`, `leaderboard(limit)`
    - Cap page size at 100, validate limit range (1–100)
    - Map entity → DTO inline
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 4.1, 4.2, 4.4, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [x] 5.3 Implement ProofQueryService
    - Create `backend/src/main/java/com/livana/backend/proof/service/ProofQueryService.java`
    - Methods: `proofsByPool(poolAddress, pageable)`, `pendingProofs(pageable)`
    - Cap page size at 100
    - Map entity → DTO inline
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 7.1, 7.2, 7.5, 7.6_

  - [x] 5.4 Implement ReputationService
    - Create `backend/src/main/java/com/livana/backend/reputation/service/ReputationService.java`
    - Methods: `getReputation(ngoAddress)`, `leaderboard(limit)`
    - Return zero values for NGO with no mints
    - Add 1-based rank to leaderboard entries
    - Validate limit range (1–100)
    - _Requirements: 8.1, 8.2, 8.3, 8.5, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x] 5.5 Implement StatsService
    - Create `backend/src/main/java/com/livana/backend/pool/service/StatsService.java`
    - Method: `getPlatformStats()`
    - Return zeros when no pools exist
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.7_

- [x] 6. Implement controller layer
  - [x] 6.1 Implement PoolController
    - Create `backend/src/main/java/com/livana/backend/pool/controller/PoolController.java`
    - `GET /api/v1/pools` with optional `region`, `search` params and `Pageable`
    - `GET /api/v1/pools/{address}` — validate address via `AddressValidator` before service call
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.3, 2.4, 11.1, 11.2, 11.3_

  - [x] 6.2 Implement DonationController
    - Create `backend/src/main/java/com/livana/backend/donation/controller/DonationController.java`
    - `GET /api/v1/donations/pool/{poolAddress}` — public, validate address
    - `GET /api/v1/donations/donor/{donorAddress}` — authenticated, validate address
    - `GET /api/v1/donations/leaderboard` — public, accept `limit` param
    - _Requirements: 3.1, 3.4, 4.1, 4.3, 4.5, 5.1, 5.4, 5.6, 11.1, 11.2, 11.3_

  - [x] 6.3 Implement ProofController
    - Create `backend/src/main/java/com/livana/backend/proof/controller/ProofController.java`
    - `GET /api/v1/proofs/pool/{poolAddress}` — public, validate address
    - `GET /api/v1/admin/proofs/pending` — `@PreAuthorize("hasRole('ADMIN')")`, paginated
    - _Requirements: 6.1, 6.4, 7.1, 7.3, 7.4, 7.7, 11.1, 11.2, 11.3_

  - [x] 6.4 Implement ReputationController
    - Create `backend/src/main/java/com/livana/backend/reputation/controller/ReputationController.java`
    - `GET /api/v1/reputation/{ngoAddress}` — public, validate address
    - `GET /api/v1/reputation/leaderboard` — public, accept `limit` param
    - _Requirements: 8.1, 8.4, 8.5, 9.1, 9.4, 9.6, 11.1, 11.2, 11.3_

  - [x] 6.5 Implement StatsController
    - Create `backend/src/main/java/com/livana/backend/pool/controller/StatsController.java`
    - `GET /api/v1/stats` — public, no params
    - _Requirements: 10.1, 10.6_

  - [x] 6.6 Update SecurityConfig to add proofs public path
    - Add `.requestMatchers(HttpMethod.GET, "/api/v1/proofs/**").permitAll()` to the security filter chain
    - Verify existing public paths cover pools, donations/leaderboard, donations/pool, stats, reputation
    - _Requirements: 3.1, 6.1, 8.5, 9.1, 10.6_

- [x] 7. Checkpoint - Compile and verify
  - Ensure all code compiles with `./mvnw compile`, ask the user if questions arise.

- [ ] 8. Property-based tests for service/repository layer
  - [ ]* 8.1 Write property test for pool listing filters (Property 3)
    - **Property 3: Pool listing filters are correct and conjunctive**
    - Generate pools with arbitrary titles/regions, seed database, query with combinations of region/search, assert every returned pool matches both filters and no valid pool is excluded
    - Use `@SpringBootTest` with Testcontainers for real DB
    - **Validates: Requirements 1.2, 1.3, 1.4**

  - [ ]* 8.2 Write property test for page size cap (Property 4)
    - **Property 4: Page size is capped at 100**
    - Generate arbitrary page size values, call service, assert effective page size is `min(requested, 100)`
    - **Validates: Requirements 1.9, 3.3, 4.4, 6.3, 7.5**

  - [ ]* 8.3 Write property test for donor leaderboard aggregation (Property 5)
    - **Property 5: Donor leaderboard aggregation is correct and ordered**
    - Generate arbitrary donation sets, persist to DB, query leaderboard, verify aggregation matches model computation (sum, count per donor), verify ordering
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.5**

  - [ ]* 8.4 Write property test for NGO reputation aggregation (Property 6)
    - **Property 6: NGO reputation aggregation is correct**
    - Generate arbitrary sbt_mint sets for an NGO, persist, query reputation endpoint, verify totalSbts = count, totalAmountReleased = sum(amount), poolCount = distinct pools
    - **Validates: Requirements 8.1, 8.2, 8.3**

  - [ ]* 8.5 Write property test for NGO leaderboard aggregation (Property 7)
    - **Property 7: NGO leaderboard aggregation is correct and ordered**
    - Generate arbitrary sbt_mint data for multiple NGOs, persist, query leaderboard, verify aggregation and ordering (totalAmountReleased DESC, ngoAddress ASC tiebreaker), verify rank is 1-based
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.5**

  - [ ]* 8.6 Write property test for platform statistics aggregation (Property 8)
    - **Property 8: Platform statistics aggregation is correct**
    - Generate arbitrary pool data with varying `totalDonated`, `totalReleased`, `isPaused`, `creatorAddress`, persist, query stats, verify sums and counts match model computation
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.7**

- [ ] 9. Integration tests with MockMvc and Testcontainers
  - [ ]* 9.1 Write integration tests for PoolController
    - Test paginated listing, region filter, search filter, combined filters, pool detail 200, pool detail 404, invalid address 400
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7, 2.1, 2.3, 2.4_

  - [ ]* 9.2 Write integration tests for DonationController
    - Test donations by pool (public), donations by donor (auth required, 401 without), leaderboard with limit, empty results
    - _Requirements: 3.1, 3.4, 3.5, 4.1, 4.3, 5.1, 5.4, 5.7_

  - [ ]* 9.3 Write integration tests for ProofController
    - Test proofs by pool (public), pending proofs (ADMIN only, 401/403 for others), empty results
    - _Requirements: 6.1, 6.4, 6.5, 7.1, 7.3, 7.4, 7.6_

  - [ ]* 9.4 Write integration tests for ReputationController and StatsController
    - Test NGO reputation (found/zeroes), NGO leaderboard, invalid address, platform stats (with/without pools)
    - _Requirements: 8.1, 8.3, 8.4, 9.1, 9.6, 9.7, 10.1, 10.6, 10.7_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Run `./mvnw test` to verify all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests and integration tests validate specific examples, edge cases, and security
- The design uses Java directly, so no language selection was needed
- AddressValidator is built first as it's shared across all controllers
- DTOs and projections are created before services (services depend on them)
- Repository query additions come before services (services call them)
- Services are built before controllers (controllers delegate to services)
- jqwik is added to pom.xml first so PBT tests can compile

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3"] },
    { "id": 3, "tasks": ["5.1", "5.2", "5.3", "5.4", "5.5"] },
    { "id": 4, "tasks": ["6.1", "6.2", "6.3", "6.4", "6.5", "6.6"] },
    { "id": 5, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6"] },
    { "id": 6, "tasks": ["9.1", "9.2", "9.3", "9.4"] }
  ]
}
```
