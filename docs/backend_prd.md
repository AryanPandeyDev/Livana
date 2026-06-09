# Livana Backend PRD

## Problem Statement

Livana has a finalized set of smart contracts (PoolFactory, FundPool, LivanaSBT) deployed on Avalanche C-Chain, governed by a Gnosis Safe multi-sig. The frontend needs a backend service to handle everything the blockchain can't: user authentication, NGO onboarding with AI screening, off-chain pool metadata, on-chain event indexing, donor profiles, and leaderboards. Without this backend, the frontend has no way to list pools with rich metadata, show donation history across pools, display NGO applications for admin review, or provide a usable onboarding flow.

## Solution

A Spring Boot backend that acts as the bridge between the Clerk-authenticated frontend and the on-chain smart contracts. It stores off-chain data (NGO applications, pool metadata, user profiles), indexes on-chain events into Postgres for fast queries, exposes REST APIs for the frontend, and integrates with an external AI screening service for NGO verification.

The backend never holds funds or makes on-chain transactions — it reads events, stores metadata, and serves APIs.

## User Stories

### Authentication & Profiles

1. As a **user**, I want to sign up via Clerk (email/Google), so that I have a persistent identity without being forced to connect a wallet immediately
2. As a **user**, I want to link my wallet address to my profile when I'm ready to donate, so that the platform can track my on-chain giving history
3. As an **NGO operator**, I want to register with my organization's details and link my wallet address before applying for verification, so that my wallet can be whitelisted on-chain
4. As an **admin**, I want to sign in via Clerk with an ADMIN role, so that I can access the admin dashboard and review NGO applications
5. As a **user**, I want my Clerk session to be validated on every API call, so that my data is protected

### NGO Application & Verification

6. As an **NGO operator**, I want to submit an application with my org name, registration number, official documents, description, and official email address, so that I can get verified on the platform
7. As an **NGO operator**, I want my official email to be verified through Clerk, so that the platform can confirm I control the email associated with my organization
8. As an **NGO operator**, I want the AI screening service to automatically verify that my organization exists and that the provided email is associated with it, so that the process is faster than pure manual review
9. As an **NGO operator**, I want to see my application status (draft ? AI screening ? pending review → approved/rejected), so that I know where I stand
10. As an **admin**, I want to see a list of all NGO applications with their status, so that I can review pending ones
11. As an **admin**, I want to see the AI screening results (confidence score, research summary, verdict) alongside the NGO's submitted documents, so that I can make an informed approval decision
12. As an **admin**, I want to approve or reject an application in our dashboard (marking intent), so that I know which NGOs to approve via the Safe multi-sig transaction
13. As an **NGO operator**, I want to be notified when my application is approved or rejected, so that I can proceed to create pools or resubmit
14. As an **admin**, I want all NGO application data (org name, description, registration docs, AI screening results) to be visible only to admins, so that sensitive organizational information is protected

### Pool Management

15. As a **verified NGO**, I want to fill in pool metadata (title, description, region, cover image, target amount) in a form, so that my pool has rich context for donors
16. As a **verified NGO**, I want the backend to upload my pool metadata to IPFS using my Pinata API key and return a CID, so that the metadata is decentralized and verifiable on-chain
17. As a **verified NGO**, I want my pool to become publicly visible once the `PoolDeployed` event is indexed and the metadata CID resolves to a valid JSON schema, so that donors can discover it
18. As a **verified NGO**, I want to upload a cover image for my pool (stored on IPFS via Pinata), so that it looks professional and trustworthy to donors
19. As a **donor**, I want to browse all active pools with their metadata (title, description, region, cover image, amount raised, target), so that I can choose where to donate
20. As a **donor**, I want to view a pool's detail page with donation history, proof submissions, release history, and NGO reputation, so that I can make an informed decision
21. As a **donor**, I want to filter/search pools by region or category, so that I can find causes I care about

### Donation Tracking

22. As a **donor**, I want to see all my donations across all pools in one place, so that I can track my giving history
23. As a **donor**, I want to see a leaderboard of top donors, so that generous giving is recognized publicly
24. As a **public visitor**, I want to see a pool's donor list and amounts, so that I can verify the pool is receiving real support
25. As a **public visitor**, I want to see platform-wide stats (total donated, total released, number of pools, number of NGOs), so that I can gauge the platform's impact

### Proof & Fund Release Tracking

26. As a **donor**, I want to see all proof submissions for a pool I donated to (IPFS CID, amount claimed, timestamp, release status), so that I can verify how my funds are being used
27. As an **NGO operator**, I want to see the status of my proof submissions (pending, released), so that I know which claims have been paid out
28. As an **admin**, I want to see pending proof submissions across all pools, so that I know what needs multi-sig review in the Safe UI

### NGO Reputation

29. As a **donor**, I want to see an NGO's reputation score (total SBTs, total funds released, number of successful pools), so that I can trust them with my donation
30. As a **public visitor**, I want to see a leaderboard of top-rated NGOs by reputation, so that I can discover trustworthy organizations
31. As an **NGO operator**, I want to see my own SBT history and cumulative reputation, so that I can showcase my track record

### Event Indexing

32. As the **system**, I want to subscribe to all contract events via WebSocket and persist them to Postgres, so that the frontend can query on-chain data without hitting the blockchain directly
33. As the **system**, I want to backfill missed events on startup (in case the backend was down), so that no data is lost
34. As the **system**, I want the event indexer to update application status to APPROVED when an `NGOApproved` event fires for a matching wallet, so that the onboarding flow is consistent

## Implementation Decisions

### Architecture

- **Framework**: Spring Boot 4.x with Java 17+ (Java 21 is preferred when convenient, but Java 17 is acceptable)
- **Database**: PostgreSQL
- **Auth**: Clerk session JWT validation through Spring Security OAuth2 Resource Server. For now, user records are created/updated lazily from verified session token claims instead of Clerk webhooks, because the backend is not hosted yet and local development should not depend on ngrok or another tunnel.
- **Blockchain**: web3j for Avalanche C-Chain event subscriptions and view-function calls
- **IPFS**: Pinata API for uploading pool metadata and cover images. The NGO provides their own Pinata API key — zero infra cost for the platform.
- **AI Screening**: External service (LangChain/LangGraph) — backend triggers via async HTTP call, stores results, and exposes them for admin review. The AI service itself is separate, but the backend integration is in scope.

### Modules

**1. Auth Module**
- Spring Security verifies Clerk JWTs on every protected request, resolves the Clerk `sub` claim to an internal user, and loads the internal role (`USER`, `NGO`, `ADMIN`) from Postgres
- On first authenticated API call, if the internal user does not exist, the backend lazily creates it from Clerk session token claims (`sub`, `primaryEmail`, `fullName`). These custom claims must be configured in Clerk, and missing required claims should fail closed instead of creating a half-authenticated transient user.
- User table stores: `clerkId`, `email`, `walletAddress` (nullable), `role`, `displayName`, `createdAt`
- `walletAddress` is optional at sign-up — the backend returns `403 WALLET_NOT_LINKED` on any endpoint that requires it, prompting the frontend to ask the user to connect their wallet
- Linking a wallet must prove ownership of that wallet. The backend should use a signed-message challenge flow before saving `walletAddress`; it must not accept a raw address string as proof that the authenticated user controls that wallet.
- Wallet addresses are normalized to lowercase before storage, and duplicate wallet conflicts return `409 WALLET_ALREADY_LINKED`, including duplicate-key races from the database uniqueness constraint.
- Clerk token validation should include the normal JWT checks handled by Spring Security (signature, expiry, issuer).
- On-chain donors who never registered are shown by their raw wallet address on leaderboards and pool donor lists — no profile link required

**Deferred Auth Work**
- Clerk webhooks are intentionally out of scope until the backend is hosted. Once hosting is available, we can add a public, signature-verified Clerk webhook endpoint for `user.created`, `user.updated`, and `user.deleted` to improve profile synchronization. The lazy session-token path should still remain as a fallback because webhook delivery is asynchronous and not guaranteed to arrive before the user's first API call.

**2. NGO Application Module**
- Application status machine: `DRAFT` → `AI_SCREENING` → `PENDING_REVIEW` → `APPROVED` / `REJECTED`
- Email verification: delegated to Clerk. The officialEmail does NOT need to match the user's primary Clerk email. The frontend guides the applicant to add and verify officialEmail through Clerk. On submit, the backend calls the Clerk Backend API to confirm officialEmail is a verified email on the authenticated Clerk user's account.
- AI screening determines whether the verified email appears official for the claimed NGO � Clerk only proves the applicant controls the inbox.
- AI screening: backend POSTs application data to the external screening service, polls or receives webhook for results, stores confidence score + research summary + verdict
- Admin endpoints: list applications (filterable by status), view single application with AI results, mark approval intent
- When `NGOApproved` event is indexed for a matching wallet address, the application status transitions to `APPROVED` automatically

**3. Pool Module**
- Pool metadata is stored on IPFS, with the CID stored on-chain in the FundPool contract's `metadataCid` field
- The metadata JSON schema is: `{ title, description, region, coverImage (IPFS CID), targetAmount }`
- Pool creation flow: NGO fills form → backend uploads cover image to IPFS via NGO's Pinata key → backend builds metadata JSON → uploads JSON to IPFS → returns CID to frontend → frontend calls `deployPool(metadataCid)` on-chain
- When `PoolDeployed` event is indexed, the indexer fetches the metadata CID from IPFS, validates the JSON schema, and caches it in Postgres for fast queries
- If the metadata CID is invalid, unreachable, or doesn't match the expected schema, the pool is **not indexed at all** — it won't appear on the platform
- Backend serves pool data from its cache — not from IPFS on every request
- Public listing endpoint with pagination, search, and region filter
- Detail endpoint joins cached metadata with on-chain indexed data (donations, proofs, releases)

**4. Event Indexer Module**
- web3j WebSocket subscription to PoolFactory and each deployed FundPool
- Events indexed: `PoolDeployed`, `DonationReceived`, `ProofSubmitted`, `FundsReleased`, `NGOApproved`, `NGORevoked`, `Locked` (SBT mint)
- Each event stored with: `txHash`, `blockNumber`, `logIndex`, `timestamp`, `contractAddress`, decoded parameters
- Tracks `lastIndexedBlock` per contract for backfill on restart
- Dynamically subscribes to new FundPool contracts as `PoolDeployed` events arrive

**5. Donation Module**
- Serves indexed `DonationReceived` data
- Endpoints: donations by pool, donations by donor, donor leaderboard (aggregated), platform-wide stats
- Donor leaderboard: total donated amount across all pools, ranked

**6. Proof & Release Module**
- Serves indexed `ProofSubmitted` and `FundsReleased` data
- Endpoints: proofs by pool (with release status), pending proofs across all pools (admin view)

**7. Reputation Module**
- Aggregates indexed SBT mint events per NGO
- Computes: total SBTs, total USDC released, number of pools with successful releases
- Endpoints: NGO reputation by wallet address, NGO reputation leaderboard

### Database Schema (conceptual)

- `users` � Clerk-backed internal user profiles with wallet address and role
- `ngo_applications` — application data, status, AI screening results
- `pool_cache` — cached IPFS metadata for indexed pools (title, description, region, cover image CID, target amount, on-chain address, creator wallet)
- `indexed_events` — raw event log (type, tx hash, block, decoded params)
- `donations` — denormalized from DonationReceived events for fast queries
- `proofs` — denormalized from ProofSubmitted + FundsReleased events
- `sbt_mints` — denormalized from Locked events for reputation queries
- `indexer_state` — tracks last processed block per contract

### API Design

All endpoints are REST (JSON). Auth via `Authorization: Bearer <clerk-jwt>`. Public endpoints (browse pools, leaderboard, platform stats) require no auth. NGO application data (all fields, AI screening results, documents) is restricted to `ADMIN` role only.

Endpoints that require an on-chain action (donate, create pool, submit proof) check that the authenticated user has a `walletAddress` linked. If not, they return `403` with error code `WALLET_NOT_LINKED`.

The wallet-linking endpoint is not just a profile update. It must issue or verify a wallet ownership challenge, then persist the normalized address only after the signed challenge is valid for the authenticated Clerk user.

The API should be versioned (`/api/v1/...`).

## Testing Decisions

Testing is deferred for now. The backend should still keep tests where they already exist or where a small focused test prevents a high-risk regression, but broad test expansion is not part of the current implementation push.

- **Test external behavior, not implementation details.** Tests should hit controller endpoints and assert HTTP responses and DB state — not internal service method signatures.
- **Auth Module**: Test JWT authentication with valid/invalid/expired tokens, missing required Clerk claims, lazy user creation, role resolution from the database, wallet ownership verification, duplicate wallet handling, and `WALLET_NOT_LINKED` responses. Clerk webhook tests are deferred until webhooks are added.
- **NGO Application Module**: Test the full status machine transitions. Test that `NGOApproved` event correctly auto-approves a matching application.
- **Pool Module**: Test that a valid metadata CID results in pool being indexed and cached. Test that an invalid/empty CID results in pool being silently ignored. Test that cached metadata matches the IPFS source.
- **Event Indexer Module**: Test event parsing and DB persistence using Foundry's Anvil as a local chain (same setup as smart contract tests).
- **Donation Module**: Test leaderboard aggregation logic.
- **Integration tests**: Full flows using a real Postgres-compatible database and Anvil local chain when the team is ready to invest in deeper regression coverage.

## Out of Scope

- **AI Screening Service implementation** — separate LangChain/LangGraph service. This PRD covers the backend interface only: trigger screening, receive or poll results, store score/summary/verdict, and expose results to admins.
- **Frontend implementation** — this PRD defines what the backend exposes, not how the frontend consumes it.
- **Smart contract deployment** — contracts are finalized and tested.
- **Safe multi-sig integration** — admins use the Safe web UI directly. The backend only reads the resulting on-chain events.
- **Email/push notifications** — can be added later.
- **Docker/containerization** — not needed for the current backend scope.
- **Pool categories/tags** — future enhancement. Region filter is sufficient for now.

## Further Notes

- The backend never initiates on-chain transactions. All state-changing blockchain interactions happen from the frontend (donor wallet → donate, NGO wallet → deployPool/submitProof) or from the Safe UI (admin → addVerifiedNGO/releaseFunds).
- Pool metadata is fully decentralized: stored on IPFS, CID anchored on-chain in the FundPool contract. The backend is just a performance cache. If the backend dies, all data can be reconstructed from contract events + IPFS.
- Pools deployed with invalid, unreachable, or malformed metadata CIDs are silently ignored by the indexer — they won't appear on the platform. The pool still functions on-chain for direct interactions.
- The NGO provides their own Pinata API key when creating a pool. The backend uses it for the upload but does not store it.
- The event indexer must handle chain reorgs gracefully — store a small confirmation buffer before treating events as final.
- The `multiSigAdmin` address can be read from `factory.multiSigAdmin()` on startup and cached.
- Avalanche C-Chain has ~2 second block times, so event indexing will be near real-time.
