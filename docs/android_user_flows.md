# Livana Android — User Flows

> Behavioral specification for the Livana Android app. Defines every action a user can take,
> the states the app can be in, and how each case (success, empty, error, edge) is handled.
>
> This is doc 1 of 3:
> 1. **User Flows** (this document, `android_user_flows.md`) — what the user can do and every case
> 2. App Requirements / PRD (`android_prd.md`) — what the app must do and how it's built
> 3. Visual Spec (`android_visual_spec.md`) — how each screen looks
>
> Companion references: `android_api_reference.md` (REST + on-chain contract), `backend_prd.md`, `scheme.md`.

---

## 1. Scope & Roles

### Roles shipped in the Android app

| Role | In app? | Notes |
|------|---------|-------|
| **Visitor** (unauthenticated) | ✅ | Can browse all public data. Cannot act. |
| **Donor** (`USER`) | ✅ | Signed in via Clerk. Can link wallet, donate, view history. |
| **NGO** (`NGO`) | ✅ | A `USER` whose wallet was approved on-chain. Adds pool creation, proof submission, reputation. |
| **Admin** (`ADMIN`) | ❌ Out of scope | Approvals and fund releases happen through the Safe multi-sig web UI, not on mobile. |

A single account can be both a donor and an NGO. Role is server-driven (`GET /users/me` → `role`) and is promoted from `USER` to `NGO` automatically when the on-chain `NGOApproved` event is indexed.

### Two infrastructure realities the flows must respect

1. **The backend is read-only.** Every state change (donate, deploy pool, submit proof) is an **on-chain transaction signed by the user's wallet**, not a backend write. The backend only sees it after the indexer picks up the event (~3–6s).
2. **Wallet ownership must be proven.** Linking a wallet uses a sign-a-challenge flow. Holding an address string is not proof.

---

## 2. App-Wide States & Conventions

These apply to every screen and flow below; individual flows reference them rather than repeating.

### 2.1 Authentication state
- **Unauthenticated** — no Clerk session. Public content visible; any gated action redirects to Sign In.
- **Authenticated, no wallet** — signed in, `walletAddress == null`. Browsing works; on-chain actions prompt wallet linking.
- **Authenticated, wallet linked** — full donor capability.
- **Authenticated NGO** — `role == NGO`. Donor capability + NGO capability.

### 2.2 Generic screen states
Every data screen must handle: **Loading**, **Loaded (content)**, **Empty** (valid response, no items), **Error** (with retry), and **Offline** (no connectivity). Lists additionally handle **paginating** (loading next page) and **end-of-list** (`last == true`).

### 2.3 On-chain transaction states
Any on-chain action moves through: **Idle → Wallet prompt (WalletConnect) → User signing → Submitted (tx hash) → Confirming → Confirmed → Indexed**. Each of **Rejected by user**, **Tx reverted**, and **Indexer not caught up yet** is an explicit case (see §10).

### 2.4 Indexer lag
After a confirmed tx, backend data can lag a few seconds. Screens that display the result poll/refresh and show a transient "Updating…" hint rather than treating absence as failure.

### 2.5 Error surfacing
All REST errors share the shape `{ errorCode, message, timestamp }`. The app maps `errorCode` to user-facing handling (see §10 error matrix). Unknown codes fall back to a generic message using `message`.

---

## 3. Navigation Map (high level)

```
App Launch
  ├─ (no session)  → Public shell: Home / Explore / Leaderboards  + "Sign In" CTA
  └─ (session)     → resolve GET /users/me
                       ├─ role USER → Donor shell
                       └─ role NGO  → NGO shell (Donor shell + NGO area)

Primary tabs (bottom nav):
  - Home/Stats     (public)
  - Explore Pools  (public, search/filter)
  - Leaderboards   (public: donors + NGOs)
  - Activity       (auth: my donations; NGO: + my pools/proofs/reputation)
  - Profile        (auth: wallet, role, settings, sign out)

NGO-only entry points appear inside Activity/Profile once role == NGO
(or while an application is in progress).
```

---

## 4. Authentication Flows

### 4.1 Sign up / Sign in (Clerk)
**Trigger:** User taps Sign In, or attempts a gated action while unauthenticated.

**Happy path:**
1. Launch Clerk SDK auth (email or Google).
2. On success, obtain Clerk **session JWT** (must carry custom claims `primaryEmail`, `fullName`).
3. App calls `GET /api/v1/users/me` with `Authorization: Bearer <jwt>`.
4. Backend lazily creates the user on first call; returns profile. App caches role + walletAddress.
5. Route to the shell matching `role`.

**Cases:**
- **Missing `primaryEmail` claim** → backend returns `401`. App shows "Account setup issue — sign out and try again," because the Clerk session token is misconfigured. Do not loop retries.
- **JWT expired / invalid** → `401` on any call → clear session, route to Sign In with "Session expired."
- **Google/email auth canceled** by user → return to previous screen, no error toast.
- **No connectivity** → show offline state on the auth screen; allow retry.
- **`USER_NOT_FOUND` (404)** on `/users/me` → should not happen with lazy creation; treat as session error → sign out + re-auth.

### 4.2 Sign out
**Trigger:** Profile → Sign Out.
1. Clerk sign-out, clear cached JWT, role, profile.
2. Disconnect any active WalletConnect session (see §5.4).
3. Return to public shell. Locally stored Pinata keys remain in Keystore unless user chose "also clear on sign out" (see §9.3).

### 4.3 Session expiry mid-use
Any 401 during use → surface a non-destructive banner "Session expired, sign in again," preserve current screen context where possible, route to Sign In. After re-auth, return to the prior screen.

---

## 5. Wallet Linking Flow

Required before any on-chain action and before applying as an NGO. Uses challenge → sign → verify.

### 5.1 Entry points
- Explicit: Profile → "Link Wallet."
- Implicit: User taps Donate / Apply as NGO / Create Pool while `walletAddress == null` → prompt to link first, then resume the original action.

### 5.2 Happy path
1. App requests `GET /api/v1/users/me/wallet/challenge` → returns `{ message }` (contains a one-time nonce, valid 5 min).
2. App ensures a WalletConnect session exists (connect wallet if not — §5.4).
3. App asks the wallet to `personal_sign` the **exact `message` string verbatim** (including newlines).
4. App sends `PATCH /api/v1/users/me/wallet` with `{ walletAddress, signature, message }`.
5. On `200`, update cached profile with the linked (lowercased) address. Resume any pending action.

### 5.3 Cases & errors
| Case | Handling |
|------|----------|
| User rejects the signature in wallet | Cancel link; return to entry point; no error toast, optional "Linking canceled." |
| Challenge expired (`CHALLENGE_EXPIRED` 400) | Auto-restart from step 1 once; if it fails again, show "Try again." |
| `NO_CHALLENGE` (400) | Re-request challenge (step 1) then retry. |
| `CHALLENGE_MISMATCH` (400) | App sent a different message than issued (e.g. wallet altered it). Re-request challenge; if persistent, show guidance that the wallet must sign the message unmodified. |
| `SIGNATURE_INVALID` (403) | Recovered signer ≠ submitted address (wrong wallet account selected). Show "That signature doesn't match the selected wallet account." Allow switching account and retry. |
| `WALLET_ALREADY_LINKED` (409) | "This wallet is already linked to another account." Offer to connect a different wallet. |
| Address format invalid (client-side) | Block before sending; should not occur via WalletConnect but validate `^0x[a-fA-F0-9]{40}$`. |
| No connectivity | Standard offline handling; preserve nonce only while valid. |

### 5.4 WalletConnect session management
- Connect: show wallet picker; establish session; cache the connected address + chain.
- **Wrong network:** if connected chain ≠ the configured target chain (Anvil `31337` now, Fuji later), prompt the user to switch networks in their wallet before any tx.
- Disconnect: from Profile, or automatically on sign out.
- Reconnect on app relaunch if a session persists; otherwise treat as not connected.
- **Account switch in wallet:** if the wallet's active account differs from the linked `walletAddress`, warn the user; on-chain actions must use the linked account (the contracts and leaderboards key off it).

---

## 6. Visitor / Donor — Browse & Discover (public)

### 6.1 Home / Platform stats
- `GET /api/v1/stats` → totals (donated, released, pools, active pools, verified NGOs).
- Cases: loading skeleton; error+retry; values formatted from USDC atomic units (÷10⁶).

### 6.2 Explore pools (list)
- `GET /api/v1/pools?page&size&region&search&sort`.
- **Search:** debounce input → `search` param. Empty results → empty state "No pools match."
- **Filter by region:** exact-match `region` param; clearable.
- **Sort:** e.g. `deployedAt,desc` (newest) — default; optionally by amount raised.
- **Pagination:** infinite scroll; stop when `last == true`; show paging spinner; handle page error without losing loaded items.
- Each card shows title, region, cover image (IPFS gateway URL from `coverImageCid`), raised vs target (progress), paused badge if `isPaused`.
- Cover image: load from `https://gateway.pinata.cloud/ipfs/<cid>`; placeholder while loading; fallback image if null/unreachable.

### 6.3 Pool detail
- `GET /api/v1/pools/{address}` → full detail incl. recent donations, recent proofs, creator reputation.
- Sections: header (title/region/cover), progress (donated/released/target), **Donate** CTA, recent donations list, proofs list (with released status), NGO reputation summary (tappable → reputation view).
- "See all donations" → `GET /api/v1/donations/pool/{address}` (paginated).
- "See all proofs" → `GET /api/v1/proofs/pool/{address}` (paginated).
- Cases:
  - `POOL_NOT_FOUND` (404) → "This pool is no longer available."
  - `INVALID_ADDRESS` (400) → should not happen from in-app navigation; treat as not found.
  - `isPaused == true` → show "Donations paused" and disable the Donate CTA.

### 6.4 Leaderboards
- Donors: `GET /api/v1/donations/leaderboard?limit`.
- NGOs: `GET /api/v1/reputation/leaderboard?limit`.
- Unregistered donors appear as raw wallet addresses (truncated display). Empty + error states standard.

### 6.5 NGO reputation view
- `GET /api/v1/reputation/{ngoAddress}` (summary) + `GET /api/v1/reputation/{ngoAddress}/history` (paginated SBT mints).
- Shows total SBTs, total released, pool count, and mint history with tx links.

---

## 7. Donor — Donate (two-transaction on-chain flow)

**Preconditions:** authenticated, wallet linked, WalletConnect connected on the correct network. Any missing precondition triggers its own sub-flow first, then resumes here.

### 7.1 Happy path
1. From pool detail, tap **Donate** → enter amount (in USDC; app converts to atomic units ×10⁶).
2. Show optional balance check via on-chain `USDC.balanceOf(wallet)` (read) — warn if amount > balance.
3. **Tx 1 — Approve:** `USDC.approve(poolAddress, amount)` via WalletConnect. Wait for confirmation.
4. **Tx 2 — Donate:** `FundPool.donate(amount)`. Wait for confirmation.
5. Show success; begin polling pool detail / `GET /donations/me` to reflect the new donation ("Updating…" until it appears, ~3–6s).

### 7.2 Cases & errors
| Case | Handling |
|------|----------|
| Wallet not linked | Redirect to §5, then resume donate. |
| Amount ≤ 0 or non-numeric | Inline validation; block. |
| Amount > USDC balance | Warn before approve; let user proceed or edit (chain would revert otherwise). |
| Existing allowance ≥ amount | Optionally skip Approve (check `USDC.allowance`); go straight to donate. |
| User rejects Approve or Donate in wallet | Cancel cleanly; keep entered amount; allow retry. |
| Approve confirmed but Donate rejected | Inform that approval succeeded; offer to retry Donate without re-approving. |
| Pool paused (`whenNotPaused` revert / `isPaused`) | Block before submitting; "Donations are paused for this pool." |
| `ZeroAmount()` revert | Should be prevented client-side; if seen, show generic tx-failed. |
| Tx reverted (other) | Show "Transaction failed" with tx hash + explorer link; no backend write occurred. |
| Wrong network | Prompt network switch to the configured target chain (Anvil now / Fuji later) before tx. |
| Indexer lag | Donation may not appear immediately; show pending hint, keep polling, allow manual refresh. |
| Connectivity lost mid-flow | If tx already submitted, it may still confirm on-chain; on reconnect, reconcile by polling. |

### 7.3 My donations
- `GET /api/v1/donations/me` (paginated) — requires linked wallet.
- `WALLET_NOT_LINKED` (403) → prompt to link wallet.
- Empty state: "You haven't donated yet" + CTA to Explore.

---

## 8. NGO Onboarding (Application) Flow

Status machine: `DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED | REJECTED`.

### 8.1 Preconditions
Authenticated + wallet linked (the wallet to be whitelisted is taken from the profile, not entered in the form).

### 8.2 Create application (DRAFT)
1. Entry: Profile/Activity → "Become an NGO / Apply."
2. If `walletAddress == null` → link wallet first (§5).
3. Form: `orgName` (≤255), `registrationNumber` (≤100), `description` (non-blank), `officialEmail` (valid email), `documentsCid` (optional — see §8.3).
4. `POST /api/v1/ngo/applications` → `201` with status `DRAFT`.

Cases:
- `WALLET_NOT_LINKED` (403) → link wallet, resume.
- `APPLICATION_ALREADY_EXISTS` (409) → user already has an active (non-terminal) application → route to existing application status (§8.6) instead of creating.
- `VALIDATION_ERROR` (400) → map field messages inline.

### 8.3 Uploading supporting documents (optional `documentsCid`)
Documents are **optional**. The client uploads supporting documents **directly to Pinata** using the NGO's own Pinata keys (the same keys held in Keystore, §9.3) and passes the resulting `documentsCid` to `POST /ngo/applications`. There is no backend document-upload endpoint; the backend only stores the CID string. If the NGO hasn't set Pinata keys, either prompt to set them (§9.3) or let them submit without documents.

### 8.4 Email verification (Clerk)
- `officialEmail` must be a **verified email on the user's Clerk account** at submit time. It does **not** need to match the primary login email.
- The app guides the NGO to add + verify `officialEmail` in Clerk (via Clerk SDK email-add/verify) before submitting.

### 8.5 Submit for review
1. `POST /api/v1/ngo/applications/me/submit` → transitions `DRAFT → AI_SCREENING`.
Cases:
- `EMAIL_NOT_VERIFIED` (400) → "Verify your official email first" → deep-link into Clerk email verification, then retry.
- `INVALID_STATUS_TRANSITION` (409) → app and server disagree on state → refresh `GET /ngo/applications/me` and reflect actual status.
- `NO_ACTIVE_APPLICATION` (404) → no draft to submit → route to create (§8.2).

### 8.6 Track status (polling)
- `GET /api/v1/ngo/applications/me` → current application (most recent, incl. terminal).
- Poll while status ∈ {`AI_SCREENING`, `PENDING_REVIEW`} (e.g. on screen focus + interval).
- Display each state with explanation:
  - `DRAFT` — "Finish and submit."
  - `AI_SCREENING` — "AI is reviewing your organization."
  - `PENDING_REVIEW` — "Awaiting admin approval."
  - `APPROVED` — "You're verified!" → unlock NGO area (also reflected by `role` flipping to `NGO`).
  - `REJECTED` — show `rejectionReason`; allow creating a new application.
- `NO_APPLICATION_FOUND` (404) → user has never applied → show the apply CTA.

### 8.7 Approval propagation
Approval is finalized on-chain via Safe (off-app). The app learns of it through:
- application status → `APPROVED`, and/or
- `GET /users/me` `role` → `NGO`.
The app should refresh both after detecting either, and reconcile if they're briefly out of sync (treat `role == NGO` OR status `APPROVED` as "NGO unlocked").

---

## 9. NGO — Create a Pool (prepare off-chain → deploy on-chain)

Three stages: upload cover image → prepare metadata → deploy on-chain. Requires `role == NGO` (approved on-chain) + linked wallet + Pinata keys.

### 9.1 Happy path
1. Entry: NGO area → "Create Pool."
2. Ensure Pinata keys are set (§9.3); ensure wallet linked + connected.
3. Form: `title`, `description`, `region`, `targetAmount` (USDC → atomic units, positive integer), cover image (file).
4. **Upload image:** `POST /api/v1/pools/upload-image` (multipart, headers: Authorization + `X-Pinata-Api-Key` + `X-Pinata-Secret-Api-Key`) → `{ cid }` (coverImage CID).
5. **Prepare metadata:** `POST /api/v1/pools/prepare` (JSON body incl. `coverImage` cid + Pinata headers) → `{ cid }` (metadata CID).
6. **Deploy on-chain:** `PoolFactory.deployPool(metadataCid)` via WalletConnect → returns pool address; wait for confirmation.
7. Poll `GET /api/v1/pools` / pool detail until the new pool is indexed (~3–6s). Show "Publishing…" until visible.

### 9.2 Cases & errors
| Stage | Case | Handling |
|-------|------|----------|
| Pre | `role != NGO` | Hide/disable Create Pool; if reached, "Only verified NGOs can create pools." |
| Image | `IMAGE_FILE_REQUIRED` (400) | Require a file. |
| Image | `IMAGE_TYPE_NOT_ALLOWED` (400) | Only JPEG/PNG/WebP. |
| Image | `IMAGE_TOO_LARGE` (400) | Max 5 MB; compress or pick smaller. |
| Image | `INVALID_REQUEST` (400) | Multiple file parts — send exactly one. |
| Image/Prepare | `PINATA_KEY_REQUIRED` (400) | Missing keys → go to Pinata setup (§9.3). |
| Image/Prepare | `PINATA_UNAUTHORIZED` (400) | Bad Pinata key → re-enter keys. |
| Image/Prepare | `PINATA_REJECTED_REQUEST` (400) | Show message; let user retry/edit. |
| Image/Prepare | `PINATA_UNREACHABLE` / `PINATA_UPSTREAM_ERROR` / `PINATA_INVALID_RESPONSE` (502) | "IPFS service issue, try again shortly." Retry. |
| Image/Prepare | `WALLET_NOT_LINKED` (403) | Link wallet (§5). |
| Image/Prepare | `NGO_NOT_APPROVED` (403) | Wallet isn't an approved NGO on-chain (e.g. revoked) → explain status. |
| Prepare | `VALIDATION_ERROR` (400) | Map field issues (title/description/region non-blank, targetAmount positive integer). |
| Deploy | `NotVerifiedNGO()` revert | Wallet not whitelisted; mirror `NGO_NOT_APPROVED` guidance. |
| Deploy | `MultiSigNotSet()` revert | Platform/config issue; "Pool creation is temporarily unavailable." |
| Deploy | `EmptyCID()` revert | Should not happen if prepare succeeded; treat as retry-from-step-5. |
| Deploy | User rejects tx | Cancel; keep form + CIDs so user can retry deploy without re-uploading. |
| Deploy | Tx reverted (other) | Show tx hash + explorer; allow retry. |
| Post | Pool not yet indexed | Pending hint + poll; if metadata were invalid the pool would never appear (backend silently drops invalid CIDs) — since the app generated valid metadata, this should resolve. |

### 9.3 Pinata keys setup (NGO settings)
- Screen to enter Pinata **API key** + **secret**.
- Stored encrypted in Android Keystore; never sent to the Livana backend except as the `X-Pinata-*` headers on upload/prepare calls.
- Validate by a lightweight test upload or by letting the first real upload surface `PINATA_UNAUTHORIZED`.
- Allow update/clear. Option to clear on sign-out.

---

## 10. NGO — Submit Proof of Impact (on-chain)

Only the pool's creator can submit proofs.

### 10.1 Happy path
1. Entry: NGO area → a pool you created → "Submit Proof."
2. Upload proof documents to IPFS (NGO Pinata keys) → obtain proof `ipfsCid`.
3. Enter claimed `amount` (USDC → atomic units, > 0).
4. `FundPool.submitProof(ipfsCid, amount)` via WalletConnect → returns proofId; wait for confirmation.
5. Poll `GET /api/v1/proofs/me` / pool proofs until the proof appears (released = false initially).

### 10.2 Cases & errors
| Case | Handling |
|------|----------|
| Not the creator (`NotCreator()` revert) | Only show Submit Proof on pools the user created; guard anyway. |
| Empty CID (`EmptyCID()`) | Require a successful doc upload first. |
| `ZeroAmount()` | Client-side validation; amount > 0. |
| Proof doc upload fails | Proof docs upload **client-direct to Pinata** (not via backend), so handle Pinata's own HTTP errors directly (auth/quota/unreachable): show "IPFS upload failed," allow retry. |
| User rejects tx | Cancel; preserve inputs; allow retry. |
| Tx reverted | tx hash + explorer; retry. |
| Indexer lag | Pending hint; poll. |
| Release status | Funds release is admin/Safe-side (off-app). Proof stays `released=false` until `FundsReleased` is indexed; reflect status changes on refresh. |

### 10.3 NGO proofs & reputation views
- My proofs across all my pools: `GET /api/v1/proofs/me` (paginated) — requires linked wallet (`WALLET_NOT_LINKED` 403 → link).
- My reputation: `GET /api/v1/reputation/{myWallet}` + history. Grows as SBTs mint on releases.

---

## 11. Profile & Settings

- View profile: `GET /users/me` (email, displayName, role, walletAddress).
- Link / **re-link** wallet (§5). Re-linking a different wallet is supported via the same challenge→sign→verify flow; address-keyed views (donations/proofs/reputation) reflect whichever wallet is currently linked.
- WalletConnect session: connect/disconnect, show connected account + network.
- Pinata keys (NGO) (§9.3).
- Sign out (§4.2).
- App info / network indicator (Fuji vs local).

---

## 12. Cross-Cutting Error & Status Matrix

| errorCode | HTTP | Where | App behavior |
|-----------|------|-------|--------------|
| `VALIDATION_ERROR` | 400 | forms | Inline field errors from `message`. |
| `INVALID_ADDRESS` | 400 | pool detail nav | Treat as not found. |
| `INVALID_PARAMETER` | 400 | leaderboard limit | Use safe default limit. |
| `INVALID_REQUEST` | 400 | image upload | One file only. |
| `IMAGE_FILE_REQUIRED` / `IMAGE_TYPE_NOT_ALLOWED` / `IMAGE_TOO_LARGE` | 400 | image upload | Specific guidance. |
| `PINATA_KEY_REQUIRED` | 400 | upload/prepare | Go to Pinata setup. |
| `NO_CHALLENGE` / `CHALLENGE_EXPIRED` / `CHALLENGE_MISMATCH` | 400 | wallet link | Re-request challenge / retry. |
| `EMAIL_NOT_VERIFIED` | 400 | NGO submit | Deep-link Clerk email verify. |
| `USER_NOT_FOUND` | 404 | /users/me | Session error → re-auth. |
| `POOL_NOT_FOUND` | 404 | pool detail | "No longer available." |
| `APPLICATION_NOT_FOUND` / `NO_APPLICATION_FOUND` | 404 | NGO status | Show apply CTA. |
| `NO_ACTIVE_APPLICATION` | 404 | NGO submit | Route to create draft. |
| `SIGNATURE_INVALID` | 403 | wallet link | Wrong account; switch + retry. |
| `WALLET_NOT_LINKED` | 403 | donate / NGO / me-history | Prompt link wallet, resume. |
| `NGO_NOT_APPROVED` | 403 | pool prepare/deploy | Explain NGO approval/revocation. |
| `APPLICATION_ALREADY_EXISTS` | 409 | NGO create | Route to existing application. |
| `INVALID_STATUS_TRANSITION` | 409 | NGO submit | Refresh actual status. |
| `WALLET_ALREADY_LINKED` | 409 | wallet link | Use a different wallet. |
| `CLERK_*` / `AI_CONFIG_*` / `INTERNAL_ERROR` | 500 | any | Generic "Something went wrong," retry; no blame on user. |
| `CLERK_EMAIL_CHECK_FAILED` | 503 | NGO submit | "Verification service busy, try again shortly." |
| `PINATA_*` | 502 | upload/prepare | "IPFS service issue," retry. |

### On-chain revert mapping
| Revert | Action | App message |
|--------|--------|-------------|
| `ZeroAmount()` | donate / submitProof | Prevent client-side. |
| `NotCreator()` | submitProof | Guarded; "Only the pool creator can do this." |
| `NotVerifiedNGO()` | deployPool | "Your NGO isn't verified (or was revoked)." |
| `MultiSigNotSet()` | deployPool | "Temporarily unavailable." |
| `EmptyCID()` | deployPool / submitProof | Retry from metadata/doc step. |
| Pausable revert | donate | "Donations are paused." |
| ERC20 allowance/balance | donate | "Check your USDC balance/allowance." |
| User rejection | any | Silent cancel + retry option. |

---

## 13. Open Questions — Resolved

All items below were resolved during the PRD phase (see `android_prd.md` §13–§14). Kept here for traceability.

1. **Application documents upload** — **RESOLVED:** client uploads docs directly to Pinata with the NGO's keys and passes `documentsCid`; documents are optional (§8.3).
2. **Re-linking / changing a wallet** — **RESOLVED:** supported via the same challenge→sign→verify flow against the new address. Address-keyed history reflects the current wallet. (§5, §11)
3. **Donation amount UX** — **RESOLVED:** free numeric entry **plus** quick-amount chips (10 / 25 / 50 / 100 USDC) with a client-side balance warning. No on-chain minimum beyond `> 0`.
4. **Region list** — **RESOLVED:** fixed enum shared by create-pool + filter (exact match): Global, Sub-Saharan Africa, North Africa, Middle East, South Asia, Southeast Asia, East Asia, Central Asia, Europe, North America, Latin America & Caribbean, Oceania.
5. **Explorer links** — **DEFERRED:** Anvil has no public explorer — show raw tx hash for now; add an explorer base URL at the testnet phase.
6. **Target chain / USDC address** — **RESOLVED:** build + test on local Anvil (chain `31337`, RPC `http://127.0.0.1:8545`, USDC `0x5FbDB2315678afecb367f032d93F642f64180aa3`, factory `0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512`). Testnet (Fuji) is a later config-only swap.
7. **Network switching** — **RESOLVED:** request switch via WalletConnect where the wallet supports it; otherwise show manual guidance (§5.4, Screen 15).
8. **Account-mismatch policy** — **RESOLVED:** warn-and-block for write actions when the wallet's active account ≠ linked address.

---

## 14. Flow Inventory (checklist)

- [ ] Launch & role resolution
- [ ] Sign up / sign in (Clerk) + claim validation
- [ ] Sign out / session expiry
- [ ] Wallet link (challenge → sign → verify) + all error cases
- [ ] WalletConnect connect/disconnect/network switch/account switch
- [ ] Home / platform stats
- [ ] Explore pools (search, filter, sort, paginate)
- [ ] Pool detail (+ paused, not found)
- [ ] Donations list per pool
- [ ] Donor leaderboard / NGO leaderboard
- [ ] NGO reputation view + history
- [ ] Donate (approve + donate, all cases)
- [ ] My donations
- [ ] NGO apply: create draft
- [ ] NGO apply: email verify (Clerk)
- [ ] NGO apply: submit + status polling
- [ ] NGO apply: rejection + resubmit
- [ ] Approval propagation (status/role reconcile)
- [ ] Pinata keys setup
- [ ] Create pool (image → prepare → deploy, all cases)
- [ ] Submit proof (upload → submitProof, all cases)
- [ ] My proofs / release status
- [ ] Profile & settings
- [ ] Global states: loading/empty/error/offline/indexer-lag
