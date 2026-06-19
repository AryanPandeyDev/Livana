# Livana Backend — Android API Reference

> Complete reference for the Android client to interact with the Livana backend.
> Base URL: `http://<server>:8080` (production TBD)

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Error Handling](#2-error-handling)
3. [Pagination](#3-pagination)
4. [User & Wallet](#4-user--wallet)
5. [Pools](#5-pools)
6. [Pool Preparation (NGO only)](#6-pool-preparation-ngo-only)
7. [Donations](#7-donations)
8. [Proofs](#8-proofs)
9. [Reputation & SBTs](#9-reputation--sbts)
10. [Platform Stats](#10-platform-stats)
11. [NGO Application](#11-ngo-application)
12. [Admin Endpoints](#12-admin-endpoints)
13. [Error Code Reference](#13-error-code-reference)
14. [Data Types & Conventions](#14-data-types--conventions)
15. [On-Chain Interactions (Smart Contracts)](#15-on-chain-interactions-smart-contracts)
16. [User Flows (REST + On-Chain)](#16-user-flows-rest--on-chain)

---

## 1. Authentication

### Auth Provider: Clerk

The backend uses **Clerk** as the identity provider. The Android app authenticates users through Clerk's SDK, which issues JWTs.

### How to Authenticate Requests

1. After Clerk login, obtain the **session token** (JWT) from the Clerk SDK
2. Attach it to every authenticated request as:
   ```
   Authorization: Bearer <clerk_session_token>
   ```

### Custom JWT Claims (Required)

The Clerk session token **must** include these custom claims (configured in Clerk Dashboard → Sessions → Customize session token):

```json
{
  "primaryEmail": "{{user.primary_email_address}}",
  "fullName": "{{user.full_name}}"
}
```

Without `primaryEmail`, the backend cannot create the user on first login and will return `401`.

### Lazy User Creation

The backend auto-creates users on their first authenticated API call using JWT claims. No explicit registration endpoint is needed.

### User Roles

| Role    | Description                                      |
|---------|--------------------------------------------------|
| `USER`  | Default role on creation. Can donate, browse.    |
| `NGO`   | Promoted after NGO application approved on-chain.|
| `ADMIN` | Can review NGO applications, manage AI config.   |

Roles are stored server-side and cannot be changed by the client.

### Public vs Authenticated Endpoints

| Pattern                               | Auth Required? |
|---------------------------------------|----------------|
| `GET /api/v1/pools/**`                | ❌ Public      |
| `GET /api/v1/donations/leaderboard`   | ❌ Public      |
| `GET /api/v1/donations/pool/**`       | ❌ Public      |
| `GET /api/v1/stats/**`                | ❌ Public      |
| `GET /api/v1/reputation/**`           | ❌ Public      |
| `GET /api/v1/proofs/**`               | ❌ Public      |
| `GET /api/v1/proofs/me`              | ✅ Required (overrides above) |
| Everything else                       | ✅ Required    |
| `/api/v1/admin/**`                    | ✅ ADMIN only  |

---

## 2. Error Handling

### Error Response Shape

All errors follow this consistent JSON structure:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "title is required",
  "timestamp": "2026-06-07T15:30:00.000+00:00"
}
```

### HTTP Status Codes

| Status | Meaning                                          |
|--------|--------------------------------------------------|
| 200    | Success                                          |
| 201    | Created (e.g., new NGO application)              |
| 400    | Validation error, invalid input                  |
| 401    | Missing or invalid JWT                           |
| 403    | Forbidden (wrong role, wallet not linked, etc.)  |
| 404    | Resource not found                               |
| 409    | Conflict (duplicate wallet link, duplicate app)  |
| 500    | Internal server error                            |
| 502    | Bad gateway (upstream Pinata errors)             |
| 503    | Upstream service unavailable (Clerk, etc.)        |

### How to Parse Errors in Android

```kotlin
data class ApiError(
    val errorCode: String,
    val message: String,
    val timestamp: String
)

// On non-2xx response:
val error = gson.fromJson(response.errorBody()?.string(), ApiError::class.java)
when (error.errorCode) {
    "WALLET_NOT_LINKED" -> navigateToWalletLinkingScreen()
    "USER_NOT_FOUND" -> handleSessionExpired()
    // ...
}
```

---

## 3. Pagination

Paginated endpoints use Spring's `Pageable` convention.

### Request Parameters

| Parameter | Type    | Default | Description                                |
|-----------|---------|---------|--------------------------------------------|
| `page`    | int     | `0`     | Zero-indexed page number                   |
| `size`    | int     | `20`    | Number of items per page                   |
| `sort`    | string  | varies  | Sort field and direction, e.g. `createdAt,desc` |

### Example

```
GET /api/v1/pools?page=0&size=10&sort=deployedAt,desc
```

### Response Shape

All paginated responses wrap content in a `Page<T>` envelope:

```json
{
  "content": [ ... ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 97,
  "last": false,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "empty": false
}
```

**Key fields for the Android client:**
- `content` — the list of items for this page
- `totalElements` — total count across all pages
- `totalPages` — total number of pages
- `last` — `true` if this is the final page (use for infinite scroll)
- `first` — `true` if this is the first page
- `number` — current page number (0-indexed)

---

## 4. User & Wallet

### `GET /api/v1/users/me` 🔒

Get the authenticated user's profile.

**Response** `200 OK`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "alice@example.com",
  "displayName": "Alice",
  "walletAddress": "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc",
  "role": "USER",
  "createdAt": "2026-06-07T10:00:00.000+00:00"
}
```

`walletAddress` is `null` if no wallet is linked yet.

**Errors:**
| Code | Status | When |
|------|--------|------|
| `USER_NOT_FOUND` | 404 | JWT subject doesn't match any user (should not happen with lazy creation) |

---

### Wallet Linking Flow

Wallet linking uses a challenge-response pattern to cryptographically prove wallet ownership.

#### Step 1: `GET /api/v1/users/me/wallet/challenge` 🔒

Request a challenge message to sign.

**Response** `200 OK`:
```json
{
  "message": "Livana Wallet Verification\n\nSign this message to prove you own this wallet.\nThis signature will not trigger a blockchain transaction.\n\nNonce: x7Hk3pQ9-aB2cD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2"
}
```

The `Nonce` is a base64url-encoded 32-byte random value (not a UUID). The challenge is valid for **5 minutes**, after which `PATCH /me/wallet` returns `CHALLENGE_EXPIRED`. Each challenge is single-use and is consumed on a successful link. Send the `message` field back **verbatim** (including newlines) in Step 3.

#### Step 2: Sign the Message

In the Android app, use the user's wallet (e.g., via Web3j or WalletConnect) to sign the `message` using `personal_sign` (EIP-191).

#### Step 3: `PATCH /api/v1/users/me/wallet` 🔒

Submit the signed challenge.

**Request Body:**
```json
{
  "walletAddress": "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
  "signature": "0x<130_hex_chars>",
  "message": "Livana Wallet Verification\n\nSign this message to prove you own this wallet.\nThis signature will not trigger a blockchain transaction.\n\nNonce: x7Hk3pQ9..."
}
```

**Validation:**
| Field           | Rule                                     |
|-----------------|------------------------------------------|
| `walletAddress` | Required, `^0x[a-fA-F0-9]{40}$`         |
| `signature`     | Required, `^0x[a-fA-F0-9]{130}$`        |
| `message`       | Required, must match the issued challenge |

**Response** `200 OK`: Returns the updated `UserProfileResponse` (same shape as `GET /me`).

**Errors:**
| Code | Status | When |
|------|--------|------|
| `NO_CHALLENGE` | 400 | No challenge was generated for this user |
| `CHALLENGE_EXPIRED` | 400 | Challenge expired (too old) |
| `CHALLENGE_MISMATCH` | 400 | Submitted message doesn't match the issued challenge |
| `SIGNATURE_INVALID` | 403 | Recovered signer ≠ claimed wallet address |
| `WALLET_ALREADY_LINKED` | 409 | This wallet is already linked to another account |

---

## 5. Pools

### `GET /api/v1/pools` 🌐 Public

List all indexed pools. Supports filtering, search, and pagination.

**Query Parameters:**
| Parameter | Type   | Required | Description                     |
|-----------|--------|----------|---------------------------------|
| `region`  | string | ❌       | Filter by region (exact match)  |
| `search`  | string | ❌       | Search in title/description     |
| `page`    | int    | ❌       | Page number (default `0`)       |
| `size`    | int    | ❌       | Page size (default `20`)        |

**Response** `200 OK` — `Page<PoolSummaryDto>`:
```json
{
  "content": [
    {
      "onChainAddress": "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
      "title": "Flood Relief Fund",
      "description": "Emergency aid for flood-affected communities...",
      "region": "South Asia",
      "coverImageCid": "QmXyz...",
      "targetAmount": 10000000000,
      "totalDonated": 5000000000,
      "totalReleased": 2000000000,
      "isPaused": false,
      "deployedAt": "2026-06-07T10:00:00.000+00:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

> **Note on amounts**: All monetary amounts are in **USDC atomic units** (6 decimals). `1000000` = 1.00 USDC. The Android app must divide by `10^6` for display.

---

### `GET /api/v1/pools/{address}` 🌐 Public

Get full pool detail including recent donations, proofs, and creator reputation.

**Path Parameters:**
| Parameter | Rule                             |
|-----------|----------------------------------|
| `address` | Valid Ethereum address (`0x...`) |

**Response** `200 OK` — `PoolDetailDto`:
```json
{
  "onChainAddress": "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
  "creatorAddress": "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc",
  "poolIndex": 0,
  "metadataCid": "QmAbc...",
  "title": "Flood Relief Fund",
  "description": "Emergency aid for flood-affected communities...",
  "region": "South Asia",
  "coverImageCid": "QmXyz...",
  "targetAmount": 10000000000,
  "totalDonated": 5000000000,
  "totalReleased": 2000000000,
  "isPaused": false,
  "deployTxHash": "0x51e405490a226...",
  "deployBlock": 15,
  "deployedAt": "2026-06-07T10:00:00.000+00:00",
  "donationCount": 12,
  "proofCount": 3,
  "recentDonations": [
    {
      "donorAddress": "0x3c44cdddb6a900...",
      "amount": 2000000000,
      "txHash": "0x2bf5b22bffdda8...",
      "blockTimestamp": "2026-06-07T12:00:00.000+00:00"
    }
  ],
  "recentProofs": [
    {
      "proofId": 1,
      "ipfsCid": "QmProof...",
      "amount": 500000000,
      "released": true,
      "submittedAt": "2026-06-07T14:00:00.000+00:00",
      "releasedAt": "2026-06-07T15:00:00.000+00:00"
    }
  ],
  "creatorReputation": {
    "ngoAddress": "0x9965507d1a55bcc...",
    "orgName": "Clean Water Foundation",
    "totalSbts": 5,
    "totalAmountReleased": 15000000000,
    "poolCount": 3
  }
}
```

**Errors:**
| Code | Status | When |
|------|--------|------|
| `POOL_NOT_FOUND` | 404 | No pool with that address |
| `INVALID_ADDRESS` | 400 | Malformed Ethereum address |

---

### Loading Cover Images from IPFS

When `coverImageCid` is non-null, load images from:
```
https://gateway.pinata.cloud/ipfs/<coverImageCid>
```

Use Coil/Glide with this URL template for image loading.

---

## 6. Pool Preparation (NGO only)

These endpoints let approved NGOs upload metadata and images to IPFS before deploying a pool on-chain.

### `POST /api/v1/pools/upload-image` 🔒 NGO

Upload a cover image to IPFS via Pinata.

**Content-Type**: `multipart/form-data`

**Headers (required):**
| Header                     | Description                        |
|----------------------------|------------------------------------|
| `Authorization`            | `Bearer <clerk_jwt>`               |
| `X-Pinata-Api-Key`         | NGO's Pinata API key               |
| `X-Pinata-Secret-Api-Key`  | NGO's Pinata API secret            |

**Form Data:**
| Field  | Type | Required | Constraints                          |
|--------|------|----------|--------------------------------------|
| `file` | File | ✅       | JPEG, PNG, or WebP. Max 5 MB.       |

**Exactly one file part is allowed** — multiple files are rejected.

**Response** `200 OK`:
```json
{
  "cid": "QmXyz123..."
}
```

**Errors:**
| Code | Status | When |
|------|--------|------|
| `IMAGE_FILE_REQUIRED` | 400 | No file or empty file |
| `IMAGE_TYPE_NOT_ALLOWED` | 400 | Not JPEG/PNG/WebP |
| `IMAGE_TOO_LARGE` | 400 | File exceeds 5 MB |
| `PINATA_KEY_REQUIRED` | 400 | Missing Pinata headers |
| `INVALID_REQUEST` | 400 | Multiple file parts |
| `WALLET_NOT_LINKED` | 403 | User has no linked wallet |
| `NGO_NOT_APPROVED` | 403 | User's wallet is not an approved NGO on-chain |
| `PINATA_UNAUTHORIZED` | 400 | Pinata rejected the supplied API key |
| `PINATA_REJECTED_REQUEST` | 400 | Pinata rejected the request (bad input) |
| `PINATA_UNREACHABLE` | 502 | Cannot connect to Pinata |
| `PINATA_UPSTREAM_ERROR` | 502 | Pinata returned a server error |
| `PINATA_INVALID_RESPONSE` | 502 | Pinata response couldn't be parsed |

---

### `POST /api/v1/pools/prepare` 🔒 NGO

Upload pool metadata JSON to IPFS. Returns the CID to use when deploying the pool on-chain.

**Content-Type**: `application/json`

**Headers (required):** Same as `/upload-image` (Pinata keys + Authorization).

**Request Body:**
```json
{
  "title": "Flood Relief Fund",
  "description": "Emergency aid for flood-affected communities in South Asia",
  "region": "South Asia",
  "coverImage": "QmXyz123...",
  "targetAmount": 10000000000
}
```

**Validation Rules:**
| Field          | Type   | Required | Constraints                            |
|----------------|--------|----------|----------------------------------------|
| `title`        | string | ✅       | Non-blank                              |
| `description`  | string | ✅       | Non-blank                              |
| `region`       | string | ✅       | Non-blank                              |
| `targetAmount` | long   | ✅       | Must be a positive JSON integer        |
| `coverImage`   | string | ❌       | IPFS CID, non-blank when present       |

**Response** `200 OK`:
```json
{
  "cid": "QmMetadata456..."
}
```

**Errors:** Same as `/upload-image` plus `VALIDATION_ERROR` for invalid metadata fields.

---

## 7. Donations

### `GET /api/v1/donations/pool/{poolAddress}` 🌐 Public

Get donations for a specific pool. Paginated.

**Response** `200 OK` — `Page<PoolDonationDto>`:
```json
{
  "content": [
    {
      "donorAddress": "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc",
      "amount": 2000000000,
      "txHash": "0x2bf5b22bffdda81b5f7438cea267...",
      "blockTimestamp": "2026-06-07T12:00:00.000+00:00"
    }
  ]
}
```

---

### `GET /api/v1/donations/me` 🔒

Get the authenticated user's donation history across all pools. Requires a linked wallet.

**Response** `200 OK` — `Page<DonorDonationDto>`:
```json
{
  "content": [
    {
      "poolAddress": "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
      "amount": 2000000000,
      "txHash": "0x2bf5b22bffdda81b5f7438cea267...",
      "blockTimestamp": "2026-06-07T12:00:00.000+00:00"
    }
  ]
}
```

**Errors:**
| Code | Status | When |
|------|--------|------|
| `WALLET_NOT_LINKED` | 403 | User has no linked wallet |

---

### `GET /api/v1/donations/leaderboard` 🌐 Public

Top donors by total donated amount.

**Query Parameters:**
| Parameter | Type | Default | Description        |
|-----------|------|---------|--------------------|
| `limit`   | int  | `10`    | Max entries to return |

**Response** `200 OK` — `List<LeaderboardEntryDto>`:
```json
[
  {
    "donorAddress": "0x3c44cdddb6a900...",
    "totalDonated": 15000000000,
    "donationCount": 7
  }
]
```

---

## 8. Proofs

### `GET /api/v1/proofs/pool/{poolAddress}` 🌐 Public

Get proof-of-impact submissions for a specific pool. Paginated.

**Response** `200 OK` — `Page<ProofDto>`:
```json
{
  "content": [
    {
      "proofId": 1,
      "ipfsCid": "QmProofDocument...",
      "amount": 500000000,
      "released": true,
      "submittedAt": "2026-06-07T14:00:00.000+00:00",
      "releasedAt": "2026-06-07T15:00:00.000+00:00"
    }
  ]
}
```

`releasedAt` is `null` if `released` is `false`.

---

### `GET /api/v1/proofs/me` 🔒 NGO

Get all proofs across all pools created by the authenticated NGO. Requires a linked wallet.

**Response** `200 OK` — `Page<NgoProofDto>`:
```json
{
  "content": [
    {
      "poolAddress": "0x9f1ac54bef0dd2f6...",
      "proofId": 1,
      "ipfsCid": "QmProofDocument...",
      "amount": 500000000,
      "released": true,
      "submittedAt": "2026-06-07T14:00:00.000+00:00",
      "releasedAt": "2026-06-07T15:00:00.000+00:00"
    }
  ]
}
```

**Errors:**
| Code | Status | When |
|------|--------|------|
| `WALLET_NOT_LINKED` | 403 | User has no linked wallet |

---

## 9. Reputation & SBTs

### `GET /api/v1/reputation/{ngoAddress}` 🌐 Public

Get aggregated reputation stats for an NGO.

**Response** `200 OK`:
```json
{
  "ngoAddress": "0x9965507d1a55bcc...",
  "orgName": "Clean Water Foundation",
  "totalSbts": 5,
  "totalAmountReleased": 15000000000,
  "poolCount": 3
}
```

`orgName` is the verified NGO's public display name (from their approved application). It is `null` when the address is not a verified NGO.

---

### `GET /api/v1/reputation/{ngoAddress}/history` 🌐 Public

Paginated SBT mint history for an NGO.

**Response** `200 OK` — `Page<SbtMintDto>`:
```json
{
  "content": [
    {
      "tokenId": 1,
      "poolAddress": "0x9f1ac54bef0dd2f6...",
      "amount": 500000000,
      "txHash": "0xabc123...",
      "blockTimestamp": "2026-06-07T15:30:00.000+00:00"
    }
  ]
}
```

---

### `GET /api/v1/reputation/leaderboard` 🌐 Public

Top NGOs ranked by reputation.

**Query Parameters:**
| Parameter | Type | Default | Description        |
|-----------|------|---------|--------------------|
| `limit`   | int  | `10`    | Max entries         |

**Response** `200 OK` — `List<NgoLeaderboardEntryDto>`:
```json
[
  {
    "ngoAddress": "0x9965507d1a55bcc...",
    "orgName": "Clean Water Foundation",
    "totalSbts": 5,
    "totalAmountReleased": 15000000000,
    "poolCount": 3,
    "rank": 1
  }
]
```

---

## 10. Platform Stats

### `GET /api/v1/stats` 🌐 Public

Aggregate platform-wide statistics for the home/dashboard screen.

**Response** `200 OK`:
```json
{
  "totalDonated": 50000000000,
  "totalReleased": 20000000000,
  "totalPoolsCount": 42,
  "activePoolsCount": 38,
  "verifiedNgosCount": 12
}
```

---

## 11. NGO Application

### Application Status Machine

```
DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED
                                       → REJECTED
```

| Status           | Meaning                                                     |
|------------------|-------------------------------------------------------------|
| `DRAFT`          | Application created but not submitted                       |
| `AI_SCREENING`   | Submitted. AI service is analyzing the application.         |
| `PENDING_REVIEW` | AI screening complete. Waiting for admin review.            |
| `APPROVED`       | Application approved. NGO is whitelisted on-chain.          |
| `REJECTED`       | Application rejected. NGO can create a new one.             |

---

### `POST /api/v1/ngo/applications` 🔒

Create a new NGO application in `DRAFT` state. Requires a linked wallet.

**Request Body:**
```json
{
  "orgName": "Clean Water Foundation",
  "registrationNumber": "NGO-2024-12345",
  "description": "We provide clean water access in rural areas...",
  "officialEmail": "contact@cleanwater.org",
  "documentsCid": "QmDocuments..."
}
```

**Validation:**
| Field                | Type   | Required | Constraints                      |
|----------------------|--------|----------|----------------------------------|
| `orgName`            | string | ✅       | Max 255 chars                    |
| `registrationNumber` | string | ✅       | Max 100 chars                    |
| `description`        | string | ✅       | Non-blank                        |
| `officialEmail`      | string | ✅       | Valid email format                |
| `documentsCid`       | string | ❌       | IPFS CID of supporting docs      |

The `walletAddress` is taken from the authenticated user's profile — not from the request body.

**Response** `201 Created`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "orgName": "Clean Water Foundation",
  "registrationNumber": "NGO-2024-12345",
  "description": "We provide clean water access...",
  "officialEmail": "contact@cleanwater.org",
  "documentsCid": "QmDocuments...",
  "walletAddress": "0x9965507d1a55bcc...",
  "status": "DRAFT",
  "createdAt": "2026-06-07T10:00:00.000+00:00",
  "updatedAt": "2026-06-07T10:00:00.000+00:00"
}
```

**Errors:**
| Code | Status | When |
|------|--------|------|
| `WALLET_NOT_LINKED` | 403 | User has no linked wallet |
| `APPLICATION_ALREADY_EXISTS` | 409 | User already has an active (non-terminal) application |

---

### `POST /api/v1/ngo/applications/me/submit` 🔒

Submit a `DRAFT` application for review. Transitions to `AI_SCREENING`.

The `officialEmail` must be a verified email on the user's Clerk account (backend checks via Clerk API).

**Response** `200 OK` — `ApplicationResponse` with `status: "AI_SCREENING"`.

**Errors:**
| Code | Status | When |
|------|--------|------|
| `NO_ACTIVE_APPLICATION` | 404 | No active (non-terminal) application for this user |
| `INVALID_STATUS_TRANSITION` | 409 | Application is not in DRAFT state |
| `EMAIL_NOT_VERIFIED` | 400 | officialEmail is not verified on Clerk |

---

### `GET /api/v1/ngo/applications/me` 🔒

Get the current user's most recent application.

**Response** `200 OK` — `ApplicationResponse`.

**Errors:**
| Code | Status | When |
|------|--------|------|
| `NO_APPLICATION_FOUND` | 404 | No application exists for this user |

---

## 12. Admin Endpoints

All admin endpoints require `ADMIN` role. Non-admin users receive `403`.

### `GET /api/v1/admin/applications` 🔒 ADMIN

List all NGO applications. Filterable by status. Paginated, default sort by `createdAt` descending.

**Query Parameters:**
| Parameter | Type                | Required | Description                |
|-----------|---------------------|----------|----------------------------|
| `status`  | ApplicationStatus   | ❌       | Filter by status enum      |

**Response** `200 OK` — `Page<AdminApplicationResponse>`:
```json
{
  "content": [
    {
      "id": "550e8400-...",
      "userId": "660f9500-...",
      "userEmail": "alice@gmail.com",
      "orgName": "Clean Water Foundation",
      "registrationNumber": "NGO-2024-12345",
      "description": "...",
      "officialEmail": "contact@cleanwater.org",
      "documentsCid": "QmDocuments...",
      "walletAddress": "0x9965507d...",
      "status": "PENDING_REVIEW",
      "aiConfidenceScore": 87.50,
      "aiResearchSummary": "The organization appears legitimate...",
      "aiVerdict": "PASS",
      "adminNotes": null,
      "rejectionReason": null,
      "createdAt": "2026-06-07T10:00:00.000+00:00",
      "updatedAt": "2026-06-07T12:00:00.000+00:00"
    }
  ]
}
```

---

### `GET /api/v1/admin/applications/{id}` 🔒 ADMIN

Get a single application with full details including AI screening results.

**Response** `200 OK` — `AdminApplicationResponse` (same shape as above).

---

### `POST /api/v1/admin/applications/{id}/approve-intent` 🔒 ADMIN

Record admin approval intent. This is an off-chain record only — actual on-chain approval happens via the Safe multi-sig UI, and the backend reacts to the `NGOApproved` event.

**Request Body (optional):**
```json
{
  "adminNotes": "Verified documentation looks good"
}
```

Body can be omitted entirely (treated as `{ "adminNotes": null }`).

**Response** `200 OK` — `AdminApplicationResponse`.

---

### `POST /api/v1/admin/applications/{id}/reject` 🔒 ADMIN

Reject an NGO application. This is a terminal state.

**Request Body:**
```json
{
  "rejectionReason": "Documentation is incomplete",
  "adminNotes": "Missing registration certificate"
}
```

| Field             | Type   | Required | Description               |
|-------------------|--------|----------|---------------------------|
| `rejectionReason` | string | ✅       | Must not be blank          |
| `adminNotes`      | string | ❌       | Optional internal notes    |

**Response** `200 OK` — `AdminApplicationResponse`.

---

### `GET /api/v1/admin/proofs/pending` 🔒 ADMIN

List all unreleased proof submissions across all pools. Used by admins to review fund release claims.

**Response** `200 OK` — `Page<PendingProofDto>`:
```json
{
  "content": [
    {
      "poolAddress": "0x9f1ac54bef0dd2f6...",
      "proofId": 1,
      "ipfsCid": "QmProofDocument...",
      "amount": 500000000,
      "submittedAt": "2026-06-07T14:00:00.000+00:00"
    }
  ]
}
```

---

### `POST /api/v1/admin/ai-config` 🔒 ADMIN

Set the Gemini API key used for AI screening of NGO applications.

**Request Body:**
```json
{
  "geminiApiKey": "AIzaSy..."
}
```

**Response** `200 OK`:
```json
{
  "configured": true,
  "maskedKey": "AIza****...Xy",
  "setBy": "user_2abc123",
  "setAt": "2026-06-07T15:00:00.000+00:00"
}
```

The raw key is **never** returned — only a masked version.

---

### `GET /api/v1/admin/ai-config` 🔒 ADMIN

Check if the Gemini API key is configured.

**Response** `200 OK` — `AiConfigStatusResponse` (same shape as above). When unconfigured:
```json
{
  "configured": false,
  "maskedKey": null,
  "setBy": null,
  "setAt": null
}
```

---

## 13. Error Code Reference

Complete list of all error codes the backend can return:

| Error Code                    | HTTP | Description                                               |
|-------------------------------|------|-----------------------------------------------------------|
| `VALIDATION_ERROR`            | 400  | Request validation failed (field errors, malformed JSON)  |
| `INVALID_ADDRESS`             | 400  | Malformed Ethereum address                                |
| `INVALID_PARAMETER`           | 400  | Generic bad parameter (e.g., leaderboard limit)           |
| `INVALID_REQUEST`             | 400  | Multiple file parts in upload                             |
| `IMAGE_FILE_REQUIRED`         | 400  | No image file provided                                    |
| `IMAGE_TYPE_NOT_ALLOWED`      | 400  | Image is not JPEG/PNG/WebP                                |
| `IMAGE_TOO_LARGE`             | 400  | Image exceeds 5 MB                                        |
| `PINATA_KEY_REQUIRED`         | 400  | Missing Pinata API headers                                |
| `NO_CHALLENGE`                | 400  | No wallet challenge generated                             |
| `CHALLENGE_EXPIRED`           | 400  | Wallet challenge expired                                  |
| `CHALLENGE_MISMATCH`          | 400  | Signed message doesn't match issued challenge             |
| `EMAIL_NOT_VERIFIED`          | 400  | Official email not verified on Clerk                      |
| `USER_NOT_FOUND`              | 404  | User doesn't exist                                        |
| `POOL_NOT_FOUND`              | 404  | Pool not found                                            |
| `APPLICATION_NOT_FOUND`       | 404  | Application not found (admin view)                        |
| `NO_APPLICATION_FOUND`        | 404  | No application for this user                              |
| `NO_ACTIVE_APPLICATION`       | 404  | No active (non-terminal) application for this user        |
| `SIGNATURE_INVALID`           | 403  | Wallet signature verification failed                      |
| `WALLET_NOT_LINKED`           | 403  | User hasn't linked a wallet yet                           |
| `NGO_NOT_APPROVED`            | 403  | Wallet is not an approved NGO on-chain                    |
| `APPLICATION_ALREADY_EXISTS`  | 409  | User already has an active application                    |
| `INVALID_STATUS_TRANSITION`   | 409  | Application status doesn't allow this action              |
| `WALLET_ALREADY_LINKED`       | 409  | Wallet already linked to another account                  |
| `CLERK_CONFIG_ERROR`          | 500  | Clerk secret key not configured                           |
| `CLERK_AUTH_ERROR`            | 500  | Clerk API authentication failed                           |
| `CLERK_USER_NOT_FOUND`        | 500  | User not found in Clerk (server-side)                     |
| `AI_CONFIG_KEY_MISSING`       | 500  | AI-config encryption key not configured (admin AI-config) |
| `AI_CONFIG_DECRYPT_ERROR`     | 500  | Failed to encrypt/decrypt the stored Gemini key           |
| `INTERNAL_ERROR`              | 500  | Unexpected server error                                   |
| `CLERK_EMAIL_CHECK_FAILED`    | 503  | Clerk API unreachable                                     |
| `PINATA_UNAUTHORIZED`         | 400  | Pinata rejected the supplied API key                      |
| `PINATA_REJECTED_REQUEST`     | 400  | Pinata rejected the request                               |
| `PINATA_UNREACHABLE`          | 502  | Cannot connect to Pinata                                  |
| `PINATA_UPSTREAM_ERROR`       | 502  | Pinata returned a server error                            |
| `PINATA_INVALID_RESPONSE`     | 502  | Pinata response couldn't be parsed                        |

---

## 14. Data Types & Conventions

### Ethereum Addresses

- Always 42 characters: `0x` + 40 hex digits
- Case-insensitive on input (backend lowercases everything)
- Pattern: `^0x[a-fA-F0-9]{40}$`

### Monetary Amounts

All amounts are in **USDC atomic units** (6 decimal places):

| Display Value | API Value      |
|---------------|----------------|
| 1.00 USDC     | `1000000`      |
| 100.00 USDC   | `100000000`    |
| 1,000.00 USDC | `1000000000`   |
| 10,000.00 USDC| `10000000000`  |

```kotlin
fun formatUsdc(atomicAmount: Long): String {
    val dollars = atomicAmount / 1_000_000.0
    return "$%.2f".format(dollars)
}
```

### Timestamps

All timestamps are ISO-8601 with timezone offset:
```
2026-06-07T15:30:00.000+00:00
```

Parse with `OffsetDateTime` or `Instant` in Kotlin:
```kotlin
val instant = Instant.parse(timestamp)
```

### IPFS CIDs

CID strings reference content on IPFS. To load:
```
https://gateway.pinata.cloud/ipfs/<cid>
```

Used for: pool metadata, cover images, proof documents, NGO documents.

### UUIDs

Entity IDs (`id`, `userId`, `applicationId`) are UUID v4 strings:
```
550e8400-e29b-41d4-a716-446655440000
```

### Null vs Absent

- `null` fields are included in JSON responses (not omitted)
- `walletAddress: null` means no wallet linked
- `releasedAt: null` means proof not yet released
- `coverImageCid: null` means no cover image

---

## 15. On-Chain Interactions (Smart Contracts)

> **Critical**: The backend is a **read-only indexer**. All write operations (donating, deploying pools, submitting proofs) are on-chain transactions that the Android app must send directly to the blockchain.

### Contract Addresses (Local Anvil Dev)

| Contract       | Address                                      |
|----------------|----------------------------------------------|
| Mock USDC      | `0x5FbDB2315678afecb367f032d93F642f64180aa3` |
| PoolFactory    | `0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512` |
| LivanaSBT      | `0xCafac3dD18aC6c6e92c921884f9E4176737C052c` |

> In production, replace these with the deployed mainnet/testnet addresses.

### RPC Configuration

| Environment | RPC URL                    | Chain ID |
|-------------|----------------------------|----------|
| Local dev   | `http://127.0.0.1:8545`    | `31337`  |
| Production  | TBD                        | TBD      |

### ABIs

Compiled ABIs are in `smartcontracts/out/<ContractName>.sol/<ContractName>.json`. Extract the `"abi"` field from each JSON file. The Android app needs ABIs for:

- **IERC20** (USDC) — `approve()`, `allowance()`, `balanceOf()`
- **PoolFactory** — `deployPool()`
- **FundPool** — `donate()`, `submitProof()`, `getPoolBalance()`, `getProof()`

---

### 15.1 Donating to a Pool

Donating is a **two-transaction flow**: first approve USDC, then call donate.

#### Step 1: Approve USDC

Call `approve()` on the USDC contract to allow the pool to pull tokens.

```solidity
// Contract: USDC (IERC20)
function approve(address spender, uint256 amount) external returns (bool)
```

| Parameter | Value                                  |
|-----------|----------------------------------------|
| `spender` | The pool's `onChainAddress`            |
| `amount`  | USDC amount in atomic units (6 dec)    |

#### Step 2: Donate

Call `donate()` on the FundPool contract.

```solidity
// Contract: FundPool (at pool's onChainAddress)
function donate(uint256 _amount) external
```

| Parameter | Value                               |
|-----------|-------------------------------------|
| `_amount` | Same amount as the approval step    |

**Reverts if:**
- `ZeroAmount()` — amount is 0
- Pool is paused (Pausable)
- Insufficient USDC allowance/balance (ERC20 revert)

**After success:** The backend indexer picks up the `DonationReceived` event and it appears in the REST API within ~3 seconds (polling interval).

---

### 15.2 Deploying a Pool (NGO only)

This is a **three-step flow**: upload image → prepare metadata → deploy on-chain.

#### Steps 1-2: REST API (covered in Section 6)

1. `POST /api/v1/pools/upload-image` → returns `coverImageCid`
2. `POST /api/v1/pools/prepare` → returns metadata `cid`

#### Step 3: Deploy On-Chain

Call `deployPool()` on the PoolFactory contract.

```solidity
// Contract: PoolFactory
function deployPool(string calldata _metadataCid) external returns (address poolAddress)
```

| Parameter      | Value                                       |
|----------------|---------------------------------------------|
| `_metadataCid` | The `cid` returned from `POST /prepare`     |

**Reverts if:**
- `NotVerifiedNGO()` — caller's wallet is not an approved NGO
- `MultiSigNotSet()` — factory not fully initialized
- `EmptyCID()` — empty metadata CID

**Returns:** The deployed pool's contract address.

**After success:** The backend indexer picks up the `PoolDeployed` event, fetches the metadata from IPFS, and creates the pool in the database. It appears in `GET /api/v1/pools` within ~3-6 seconds.

---

### 15.3 Submitting a Proof (NGO only)

Only the pool's creator (NGO) can submit proofs.

```solidity
// Contract: FundPool (at pool's onChainAddress)
function submitProof(string calldata _ipfsCid, uint256 _amount) external returns (uint256 proofId)
```

| Parameter  | Value                                              |
|------------|----------------------------------------------------|
| `_ipfsCid` | IPFS CID of proof documents (uploaded separately)  |
| `_amount`  | Claimed reimbursement amount (USDC atomic units)   |

**Reverts if:**
- `NotCreator()` — caller is not the pool's creator
- `EmptyCID()` — empty IPFS CID
- `ZeroAmount()` — amount is 0

**After success:** The indexer picks up `ProofSubmitted` and it appears in `GET /api/v1/proofs/pool/{address}`.

---

### 15.4 Reading On-Chain Data (View Functions)

These are free calls (no gas) useful for real-time data before the indexer catches up:

```solidity
// FundPool
function getPoolBalance() external view returns (uint256)       // Current USDC balance
function totalDonated() external view returns (uint256)          // Cumulative donated
function totalReleased() external view returns (uint256)         // Cumulative released
function proofCount() external view returns (uint256)            // Number of proofs
function getProof(uint256 _proofId) external view returns (
    string memory ipfsCid, uint256 amount, uint256 timestamp, bool released
)

// PoolFactory
function isVerified(address _ngo) external view returns (bool)  // Check NGO status
function poolCount() external view returns (uint256)             // Total pools deployed

// IERC20 (USDC)
function balanceOf(address account) external view returns (uint256)
function allowance(address owner, address spender) external view returns (uint256)
```

---

### 15.5 USDC Balance Display

To show the user's USDC balance before donating:

```kotlin
// Call USDC.balanceOf(userWalletAddress)
// Returns atomic units — divide by 1_000_000 for display
val balance = usdcContract.balanceOf(walletAddress).send()
val displayBalance = balance.toBigDecimal().divide(BigDecimal(1_000_000), 2, RoundingMode.DOWN)
```

---

## 16. User Flows (REST + On-Chain)

### 🧑 Donor Flow

```
1. Sign in with Clerk                          → Clerk SDK
2. GET /api/v1/users/me                        → Check if wallet linked
3. Link wallet (if needed)                     → GET challenge → sign → PATCH wallet
4. GET /api/v1/pools                           → Browse pools
5. GET /api/v1/pools/{address}                 → View pool detail
6. USDC.approve(poolAddress, amount)           → ⛓️ On-chain tx
7. FundPool.donate(amount)                     → ⛓️ On-chain tx
8. GET /api/v1/donations/me                    → View my donations
9. GET /api/v1/donations/leaderboard           → View leaderboard
```

### 🏢 NGO Flow

```
1. Sign in + link wallet                       → Same as donor steps 1-3
2. POST /api/v1/ngo/applications               → Create application
3. POST /api/v1/ngo/applications/me/submit     → Submit for review
4. GET /api/v1/ngo/applications/me             → Poll status until APPROVED
--- After approval ---
5. POST /api/v1/pools/upload-image             → Upload cover image to IPFS
6. POST /api/v1/pools/prepare                  → Upload metadata to IPFS → get CID
7. PoolFactory.deployPool(metadataCid)         → ⛓️ Deploy pool on-chain
8. FundPool.submitProof(ipfsCid, amount)       → ⛓️ Submit proof on-chain
9. GET /api/v1/proofs/me                       → View my proofs & release status
10. GET /api/v1/reputation/{myAddress}         → View my reputation
```

### 👨‍💼 Admin Flow

```
1. Sign in (ADMIN role)                        → Clerk SDK
2. GET /api/v1/admin/applications?status=PENDING_REVIEW  → Review queue
3. GET /api/v1/admin/applications/{id}         → View full details + AI results
4. POST /api/v1/admin/applications/{id}/approve-intent   → Record intent
5. --- Sign via Safe multi-sig UI ---          → ⛓️ On-chain (not in Android app)
6. POST /api/v1/admin/applications/{id}/reject → Or reject
7. GET /api/v1/admin/proofs/pending            → Review pending proofs
8. --- Release via Safe multi-sig UI ---       → ⛓️ On-chain (not in Android app)
```

### Key Architecture Insight

```
┌─────────────┐     REST API      ┌──────────────┐     Indexer      ┌────────────┐
│  Android    │ ─────────────────→ │   Backend    │ ←──────────────  │ Blockchain │
│  App        │ ←───────────────── │   (read)     │   polls events   │  (Anvil/   │
│             │                    └──────────────┘                  │  mainnet)  │
│             │     On-chain tx (Web3j / WalletConnect)              │            │
│             │ ────────────────────────────────────────────────────→ │            │
└─────────────┘                                                      └────────────┘
```

- **Backend** = read-only. Queries go here.
- **Blockchain** = write-only (from app's perspective). State changes go here.
- **Indexer** bridges the two: on-chain events → backend database → REST API.
- Expect **~3-6 second delay** between an on-chain tx confirming and the data appearing in REST API responses.
