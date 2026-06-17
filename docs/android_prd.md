# Livana Android — Product Requirements (PRD)

> Doc 2 of 3. Defines **what the Android app must do and how it is built**.
> Derives from `android_user_flows.md` (behavior) and `android_api_reference.md` (contract).
> Companion: `backend_prd.md`, `scheme.md`, `smartcontracts/`.
>
> Doc 3 (Visual Spec, `android_visual_spec.md`) defines how each screen looks.

---

## 1. Overview

### 1.1 Purpose
A native Android client for Livana, a blockchain-based charitable funding platform. The app lets the public discover causes, lets donors give USDC on-chain with full transparency, and lets verified NGOs onboard, create funding pools, and submit proof-of-impact — all backed by the Livana Spring Boot backend (read APIs) and the on-chain contracts (write actions).

### 1.2 Product principles
- **Transparency first.** Every donation, proof, and release is on-chain and visible.
- **The phone never custodies funds via the app.** Users sign with their own external wallet.
- **The backend is a read cache.** All writes are on-chain transactions; the app reconciles via the indexer.
- **Browse freely, act when ready.** Public content needs no account; actions require auth + a linked wallet.

### 1.3 Target environment (phased)
| Phase | Chain | RPC | Notes |
|-------|-------|-----|-------|
| **Now (build + test)** | Local **Anvil**, chainId `31337` | `http://127.0.0.1:8545` (emulator: `http://10.0.2.2:8545`) | Deterministic dev addresses. Backend at `http://10.0.2.2:8080`. |
| **Later** | Avalanche **Fuji** testnet | Fuji RPC | Config swap only — no code changes. |
| **Future** | Avalanche mainnet | mainnet RPC | Config swap only. |

All chain/network/contract/base-URL values are **configuration**, never hardcoded in logic (see §9).

Local Anvil contract addresses (from backend `application.properties`):
- USDC (mock): `0x5FbDB2315678afecb367f032d93F642f64180aa3`
- PoolFactory: `0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512`
- LivanaSBT: `0xCafac3dD18aC6c6e92c921884f9E4176737C052c`

---

## 2. Scope

### 2.1 In scope (v1)
- Roles: **Visitor** (unauth), **Donor** (`USER`), **NGO** (`NGO`).
- Clerk authentication (email + Google) via Clerk Android SDK.
- Wallet linking + **re-linking** via signed-challenge (WalletConnect).
- Public browse: home/stats, explore pools (search/filter/sort/paginate), pool detail, leaderboards, NGO reputation.
- Donor: donate (approve + donate), my donations.
- NGO: application + Clerk email verification + status tracking; Pinata key setup; create pool (image → prepare → deploy); submit proof; my proofs; my reputation.
- On-chain reads (Web3j) and writes (WalletConnect).
- Offline/error/loading/empty/indexer-lag handling per flows doc.

### 2.2 Out of scope (v1)
- **Admin role** (approvals/releases happen in the Safe multi-sig web UI).
- Push/email notifications (polling only).
- In-app embedded/custodial wallet.
- Fiat on-ramp / buying USDC.
- Tablet-optimized layouts; landscape (phone portrait only).
- Localization beyond English (structure for it, don't translate yet).

### 2.3 Assumptions / dependencies
- Backend reachable and indexer running.
- Smart contracts deployed; ABIs available from `smartcontracts/out/<Name>.sol/<Name>.json`.
- Clerk app configured with custom session claims `primaryEmail`, `fullName`.
- User has an external wallet supporting WalletConnect on the target chain, funded with test USDC + gas.

---

## 3. Personas
- **Visitor** — exploring causes; not signed in.
- **Donor (USER)** — signed in, wants to give and track giving.
- **NGO operator** — approved organization creating pools and proving impact.
(Admin is served by the existing web/Safe tooling, not this app.)

---

## 4. Architecture

### 4.1 Style: Clean Architecture
Three layers with strict dependency direction (presentation → domain ← data; domain depends on nothing):

```
presentation/   Compose UI, ViewModels (MVVM), navigation, UI state & events
   │  (depends on domain)
domain/         Entities, UseCases (interactors), Repository interfaces, value types
   ▲  (pure Kotlin, no Android/framework deps)
data/           Repository implementations, REST (Retrofit), chain (Web3j),
                wallet (WalletConnect), local (Keystore/DataStore), DTOs + mappers
   │  (implements domain interfaces)
```

- **Domain** is pure Kotlin: entities (Pool, Donation, Proof, NgoApplication, UserProfile, Reputation, Money/Usdc value type, WalletAddress value type), repository interfaces, and use cases (e.g. `GetPoolsUseCase`, `DonateUseCase`, `LinkWalletUseCase`, `SubmitProofUseCase`).
- **Data** implements repositories, owns DTOs and DTO↔domain mappers, network/chain/local sources. No domain or UI type leaks a DTO.
- **Presentation** holds one ViewModel per screen exposing immutable UI state + one-off events; no business logic in composables.
- **DI** via Hilt. Each layer wired through modules; feature modules optional.

### 4.2 Package structure — feature-first + shared `core`

**Decision: package by feature** (consistent with the backend), with a shared `core/` package for
everything used across features. Single Gradle module now; packages drawn so any `core/*` or
`feature/*` can be promoted to its own module later (Now-in-Android pattern). The full tree and
rationale live in `android/PROJECT_STRUCTURE.md` — this is the summary:

```
com/livana/app/
  LivanaApplication.kt · MainActivity.kt · di/        // Hilt modules
  core/
    designsystem/ theme/ (Color,Type,Shape,Dimens,Theme) + component/ (BrandMark + reusables)
    model/        // shared domain entities (Usdc, Region, Pool, Donation, Proof, …)
    common/       // Result, DomainError, dispatchers, formatters
    network/      // Retrofit/OkHttp, auth interceptor, Page<T>, error parser, dto/, mapper/
    chain/        // Web3j provider, TransactionEngine, RevertMapper, WalletConnect, contracts/
    auth/         // Clerk session/token provider
    storage/      // Keystore secrets, Pinata key store, DataStore
    data/repository/  // shared repositories (Pool/Donation/Proof/Reputation/User/NgoApplication/Stats)
    navigation/   // Destinations + LivanaNavHost + bottom bar
    ui/           // shared stateful composables (skeletons, empty/error/offline, sheets, tx sheet)
  feature/
    home/ explore/ pooldetail/ donate/ leaderboard/ reputation/ activity/
    profile/ settings/ wallet/ ngoapply/ ngopool/ ngoproof/ pinata/
      // each: XScreen.kt + XViewModel.kt + XUiState.kt (+ feature-only use-cases)
```

Why feature-first over layer-first: a layer-first split (top-level `domain/`, `data/`,
`presentation/`) doesn't scale — every feature dumps into three growing folders. Feature-first keeps
each feature cohesive and movable; shared models/repos/infra sit in `core/` so they aren't
duplicated. Clean Architecture still holds: `feature(ui)` → `core/model` + repository interfaces;
`core/data` implements them via `core/network` + `core/chain` + `core/storage`; UI never sees DTOs.

### 4.3 Key technology choices
| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Async | Coroutines + Flow |
| DI | Hilt |
| Navigation | Navigation-Compose (type-safe routes) |
| REST | Retrofit + OkHttp + kotlinx.serialization (or Moshi) |
| Images | Coil (IPFS gateway URLs) |
| Chain reads/encoding | Web3j |
| Wallet signing | WalletConnect v2 (Reown AppKit/WalletKit) |
| Auth | Clerk Android SDK |
| Secure storage | Android Keystore (+ EncryptedSharedPreferences/DataStore) |
| Prefs/session | DataStore |
| Min SDK | 26 (Android 8.0) |
| Target/compile SDK | latest stable |
| Orientation | Portrait, phone |

---

## 5. Functional Requirements

Requirements use IDs (`FR-x.y`). Each lists acceptance criteria. Behavior detail and edge cases live in the flows doc; this section is the authoritative "must" list.

### 5.1 Authentication (FR-1)
- **FR-1.1** The app SHALL authenticate users via Clerk (email and Google) and obtain a session JWT.
- **FR-1.2** The app SHALL attach `Authorization: Bearer <jwt>` to every authenticated request.
- **FR-1.3** On first authenticated call the app SHALL call `GET /api/v1/users/me` and cache `role` and `walletAddress`.
- **FR-1.4** WHEN any request returns `401`, the app SHALL clear the session and route to Sign In with a "session expired" message, preserving return context.
- **FR-1.5** IF the Clerk token lacks required claims (backend `401` on lazy creation), the app SHALL show an account-setup error and SHALL NOT retry-loop.
- **FR-1.6** The app SHALL allow sign out, clearing JWT, cached profile, and active WalletConnect session.
- **FR-1.7** Public screens SHALL be fully usable without a session.

### 5.2 Wallet linking & WalletConnect (FR-2)
- **FR-2.1** The app SHALL connect to an external wallet via WalletConnect v2 and persist/restore the session across launches.
- **FR-2.2** To link a wallet the app SHALL: `GET /users/me/wallet/challenge`, have the wallet `personal_sign` the returned `message` **verbatim**, then `PATCH /users/me/wallet` with `{ walletAddress, signature, message }`.
- **FR-2.3** The app SHALL support **re-linking** a different wallet via the same challenge→sign→verify flow; on success the cached `walletAddress` is replaced and address-keyed views reflect the new wallet.
- **FR-2.4** The app SHALL handle link errors per the matrix: `NO_CHALLENGE`, `CHALLENGE_EXPIRED` (auto re-request once), `CHALLENGE_MISMATCH`, `SIGNATURE_INVALID` (403), `WALLET_ALREADY_LINKED` (409).
- **FR-2.5** WHEN the connected wallet is on the wrong network, the app SHALL prompt a network switch before any transaction.
- **FR-2.6** WHEN the wallet's active account ≠ linked `walletAddress`, the app SHALL warn the user and SHALL require the linked account for on-chain actions (decision: warn-and-block for write actions).
- **FR-2.7** The app SHALL validate `walletAddress` (`^0x[a-fA-F0-9]{40}$`) and signature (`^0x[a-fA-F0-9]{130}$`) client-side before submitting.

### 5.3 Browse & discovery (public) (FR-3)
- **FR-3.1** The app SHALL show platform stats from `GET /api/v1/stats`.
- **FR-3.2** The app SHALL list pools via `GET /api/v1/pools` with `region`, `search`, `page`, `size`, `sort`; support debounced search, region filter, sort (default `deployedAt,desc`), and infinite-scroll pagination honoring `last`.
- **FR-3.3** The app SHALL render pool detail from `GET /api/v1/pools/{address}`, including recent donations, recent proofs, and creator reputation; disable Donate when `isPaused`.
- **FR-3.4** The app SHALL paginate per-pool donations (`/donations/pool/{address}`) and proofs (`/proofs/pool/{address}`).
- **FR-3.5** The app SHALL show donor leaderboard (`/donations/leaderboard`) and NGO leaderboard (`/reputation/leaderboard`) with a `limit`.
- **FR-3.6** The app SHALL show NGO reputation (`/reputation/{ngoAddress}`) and history (`/reputation/{ngoAddress}/history`).
- **FR-3.7** The app SHALL load IPFS images from `<ipfs-gateway>/<cid>` with placeholder + fallback; handle null CIDs.
- **FR-3.8** All list/detail screens SHALL implement loading, empty, error+retry, and offline states.

### 5.4 Donations (FR-4)
- **FR-4.1** The app SHALL require auth + linked wallet + correct network before donating; missing preconditions trigger their sub-flows and resume.
- **FR-4.2** The app SHALL accept a USDC amount, convert to atomic units (×10⁶), and validate `> 0`.
- **FR-4.3** The app SHOULD read `USDC.balanceOf` and `USDC.allowance` to warn on insufficient balance and to skip `approve` when allowance already covers the amount.
- **FR-4.4** The app SHALL execute `USDC.approve(pool, amount)` then `FundPool.donate(amount)` via WalletConnect, surfacing each tx's submitted/confirming/confirmed state.
- **FR-4.5** The app SHALL handle: user rejection, approve-ok-but-donate-rejected (retry donate without re-approving), paused pool, revert, wrong network, and indexer lag (poll to reflect the new donation).
- **FR-4.6** The app SHALL show my donations via `GET /donations/me`; handle `WALLET_NOT_LINKED` and empty state.

### 5.5 NGO application (FR-5)
- **FR-5.1** The app SHALL require auth + linked wallet to create an application; the whitelisted wallet is taken from profile (not entered).
- **FR-5.2** The app SHALL create a draft via `POST /ngo/applications` with validated fields (`orgName` ≤255, `registrationNumber` ≤100, `description` non-blank, `officialEmail` valid email, optional `documentsCid`).
- **FR-5.3** WHEN `APPLICATION_ALREADY_EXISTS` (409), the app SHALL route to the existing application status instead of creating.
- **FR-5.4** The app SHALL guide the user to add + verify `officialEmail` in Clerk; it need not equal the login email.
- **FR-5.5** The app SHALL submit via `POST /ngo/applications/me/submit`; handle `EMAIL_NOT_VERIFIED` (deep-link Clerk verify), `INVALID_STATUS_TRANSITION` (refresh state), `NO_ACTIVE_APPLICATION` (route to create).
- **FR-5.6** The app SHALL track status via `GET /ngo/applications/me`, polling while `AI_SCREENING`/`PENDING_REVIEW`, and render each state (incl. `REJECTED` with `rejectionReason` + resubmit, `NO_APPLICATION_FOUND` → apply CTA).
- **FR-5.7** The app SHALL treat `role == NGO` (from `/users/me`) OR application `APPROVED` as "NGO unlocked," reconciling the two.

### 5.6 Pinata key management (FR-6)
- **FR-6.1** The app SHALL let an NGO store a Pinata API key + secret encrypted in Android Keystore.
- **FR-6.2** The app SHALL send Pinata keys only as `X-Pinata-Api-Key` / `X-Pinata-Secret-Api-Key` headers on `/pools/upload-image` and `/pools/prepare`; never to any other endpoint and never logged.
- **FR-6.3** The app SHALL allow updating/clearing keys, and optionally clearing them on sign out.
- **FR-6.4** WHEN an upload returns `PINATA_KEY_REQUIRED` or `PINATA_UNAUTHORIZED`, the app SHALL route to Pinata setup.

### 5.7 Pool creation (NGO) (FR-7)
- **FR-7.1** The app SHALL gate pool creation on `role == NGO` + linked wallet + Pinata keys set.
- **FR-7.2** The app SHALL upload a cover image (`POST /pools/upload-image`, one JPEG/PNG/WebP ≤5MB) → coverImage CID; handle `IMAGE_FILE_REQUIRED`/`IMAGE_TYPE_NOT_ALLOWED`/`IMAGE_TOO_LARGE`/`INVALID_REQUEST`.
- **FR-7.3** The app SHALL prepare metadata (`POST /pools/prepare`, body `{title, description, region, coverImage?, targetAmount>0}`) → metadata CID; handle `VALIDATION_ERROR` + Pinata errors.
- **FR-7.4** The app SHALL deploy on-chain via `PoolFactory.deployPool(metadataCid)`; handle reverts `NotVerifiedNGO`, `MultiSigNotSet`, `EmptyCID`, user rejection (preserve CIDs to retry deploy without re-upload), and other reverts.
- **FR-7.5** After deploy the app SHALL poll until the pool is indexed and visible.

### 5.8 Proof submission (NGO) (FR-8)
- **FR-8.1** The app SHALL show Submit Proof only on pools the user created.
- **FR-8.2** The app SHALL upload proof documents to IPFS (NGO Pinata keys) → proof CID.
- **FR-8.3** The app SHALL submit via `FundPool.submitProof(ipfsCid, amount>0)`; handle reverts `NotCreator`, `EmptyCID`, `ZeroAmount`, user rejection (preserve inputs), other reverts; poll to reflect the new proof.
- **FR-8.4** The app SHALL show my proofs via `GET /proofs/me` (handle `WALLET_NOT_LINKED`) with release status updating on refresh.

### 5.9 Reputation (NGO self) (FR-9)
- **FR-9.1** The app SHALL show the NGO's own reputation + SBT history, growing as `FundsReleased`/SBT mints are indexed.

### 5.10 Profile & settings (FR-10)
- **FR-10.1** The app SHALL show profile (email, displayName, role, walletAddress).
- **FR-10.2** The app SHALL expose: link/re-link wallet, WalletConnect connect/disconnect + connected account/network, Pinata keys (NGO), sign out, and current network indicator.

---

## 6. On-Chain Integration Requirements

### 6.1 Contracts & ABIs
- ABIs sourced from `smartcontracts/out/<Name>.sol/<Name>.json` (extract `abi`). Needed: `IERC20` (USDC), `PoolFactory`, `FundPool`. (`LivanaSBT` reads optional.)
- Contract addresses are config (per environment). Pool addresses come from API (`onChainAddress`).

### 6.2 Reads (Web3j, gasless `eth_call`)
- `USDC.balanceOf(addr)`, `USDC.allowance(owner, spender)`.
- Optional real-time pre-indexer reads: `FundPool.getPoolBalance/totalDonated/totalReleased/proofCount/getProof`, `PoolFactory.isVerified(addr)`.

### 6.3 Writes (WalletConnect, user-signed)
- `USDC.approve(spender, amount)`, `FundPool.donate(amount)`, `PoolFactory.deployPool(metadataCid)`, `FundPool.submitProof(ipfsCid, amount)`, and `personal_sign` for wallet linking.
- The app builds calldata (Web3j ABI encoding) and sends via WalletConnect `eth_sendTransaction`; it never holds private keys.

### 6.4 Transaction engine (core-chain)
- Single component manages: build → request signature → submit → poll receipt → map status → trigger indexer reconciliation.
- Maps known custom-error selectors / revert reasons to user messages (see flows §12).
- Exposes tx state as a Flow for the ViewModel.
- Handles user rejection, timeout, and reorg-tolerant confirmation (confirmations configurable; `0` on Anvil).

### 6.5 Amounts & precision
- All USDC values are integers in atomic units (6 decimals) end-to-end; only the presentation layer formats to decimal. Never use floating point for on-chain amounts (use `BigInteger`/`BigDecimal`).

---

## 7. Data / Network Layer Requirements

- **NR-1** A single Retrofit/OkHttp client with: auth interceptor (injects JWT, refreshes via Clerk when needed), logging (no secrets/keys/tokens), timeouts, and base-URL from config.
- **NR-2** A generic `Page<T>` model maps the Spring pagination envelope (`content`, `totalElements`, `totalPages`, `last`, `first`, `number`, `size`, `numberOfElements`, `empty`).
- **NR-3** A central error parser maps `{errorCode, message, timestamp}` (and HTTP status) into a typed `DomainError`; ViewModels switch on it (see flows §12).
- **NR-4** DTOs are data-layer only; mappers convert to domain entities. UI never sees DTOs.
- **NR-5** Repositories expose suspend functions / Flows returning `Result<Domain, DomainError>`; no raw Retrofit types above data layer.
- **NR-6** Pinata header injection is per-call (only on upload/prepare), sourced from Keystore, never cached in the shared client.
- **NR-7** Caching: in-memory for the session; optional short-lived disk cache for pool list/detail; always provide pull-to-refresh.

---

## 8. Non-Functional Requirements

### 8.1 Security
- **SEC-1** No private keys in the app; signing is delegated to the external wallet.
- **SEC-2** Pinata keys and WalletConnect session data stored via Android Keystore-backed encryption; never logged, never sent to non-Pinata endpoints.
- **SEC-3** JWTs kept in memory / encrypted storage; cleared on sign out and on 401.
- **SEC-4** No secrets in logs, crash reports, or analytics. Release builds strip verbose logging.
- **SEC-5** Network over HTTPS in testnet/prod; cleartext allowed only for local Anvil/backend in debug builds via a scoped network-security config.
- **SEC-6** Validate all user input client-side (addresses, amounts, file type/size) before network/chain calls.
- **SEC-7** Treat all backend/chain/IPFS responses as untrusted input; defensive parsing.

### 8.2 Performance
- **PERF-1** First meaningful content on Home/Explore < 2s on a warm network.
- **PERF-2** Smooth 60fps lists; paginate at `size=20`; images lazy-loaded + cached (Coil).
- **PERF-3** Avoid redundant network calls (cache + refresh policy); cancel in-flight calls on navigation away.
- **PERF-4** Post-tx polling backs off (e.g. 2s → 5s) and stops after a bounded window with manual refresh fallback.

### 8.3 Reliability / offline
- **REL-1** Every screen handles offline gracefully (cached content where available + retry).
- **REL-2** On-chain tx already submitted SHALL be reconciled by polling on reconnect (don't assume failure).
- **REL-3** Idempotent UI: re-entering a flow after a crash/restart restores or safely restarts it.

### 8.4 Accessibility
- **A11Y-1** All actionable elements have content descriptions; min 48dp touch targets.
- **A11Y-2** Support dynamic font scaling and TalkBack navigation order.
- **A11Y-3** Color contrast meets WCAG AA; never rely on color alone (e.g. paused/released also use text/icon).
> Full WCAG conformance requires manual testing with assistive tech and expert review; this lists targets, not a guarantee.

### 8.5 Observability
- **OBS-1** Structured, secret-free logging; user-facing errors carry a copyable correlation/tx hash where relevant.
- **OBS-2** Optional crash reporting (e.g. Crashlytics) with PII/secret scrubbing — off by default until configured.

### 8.6 Compatibility
- Min SDK 26, portrait phone, **single light theme only** (no dark mode; see Visual Spec §1). Colors are defined as tokens so a dark theme could be added later, but v1 ships light only.

---

## 9. Configuration & Build

- **CFG-1** Use build flavors / build config fields + a typed `AppConfig` for: `baseUrl`, `chainId`, `rpcUrl`, `ipfsGatewayUrl`, contract addresses (USDC/factory/SBT), WalletConnect project ID, Clerk publishable key, explorer base URL (nullable on Anvil).
- **CFG-2** Provide at least two configs: `anvilDebug` (current) and `testnet` (Fuji, later). Switching environments requires no logic changes.
- **CFG-3** Emulator networking: backend `http://10.0.2.2:8080`, Anvil `http://10.0.2.2:8545`; document device/host alternatives.
- **CFG-4** Secrets (WalletConnect project ID, Clerk key) injected via Gradle properties / `local.properties` / CI secrets — not committed.
- **CFG-5** Debug builds allow cleartext to local hosts via a scoped `network_security_config`; release builds disallow cleartext.

---

## 10. Navigation & State

- **NAV-1** Navigation-Compose with a bottom-nav shell: Home, Explore, Leaderboards, Activity, Profile. NGO entry points appear in Activity/Profile when NGO-unlocked.
- **NAV-2** Gated destinations redirect unauthenticated users to Sign In and resume the original intent on success.
- **NAV-3** Each screen has one ViewModel exposing immutable UI state + a one-shot event channel (navigation, toasts, wallet prompts).
- **NAV-4** Deep, resumable flows (donate, link wallet, create pool, submit proof) survive process death (save form state + step).

---

## 11. Testing Strategy

- **TEST-1** Domain use cases: pure unit tests (no Android).
- **TEST-2** Data mappers + error parser: unit tests against representative DTO/error JSON (including every `errorCode`).
- **TEST-3** Repositories: tests with a mock web server (OkHttp MockWebServer) covering success, each error code, and pagination.
- **TEST-4** ViewModels: state-transition tests with fake use cases (loading/empty/error/offline/indexer-lag).
- **TEST-5** Chain layer: unit tests for ABI encoding + revert-selector mapping; instrumented/integration tests against local Anvil for read calls and the donate/deploy/submitProof happy paths.
- **TEST-6** Critical UI: Compose UI tests for donate flow, wallet linking, and NGO create-pool gating.
- **TEST-7** Manual E2E checklist on Anvil before any testnet deploy (mirrors flows §14 inventory).
> Depth of automated coverage can scale over time; prioritize the money-path (donate) and trust-path (wallet link, NGO gating) first.

---

## 12. Acceptance (v1 "done") Checklist
- [ ] Visitor can browse stats, pools (search/filter/sort/paginate), pool detail, leaderboards, reputation — no account.
- [ ] User can sign in/out via Clerk; session + 401 handling correct.
- [ ] User can link and re-link a wallet via signed challenge; all link errors handled.
- [ ] Donor can donate (approve + donate) and see it reflected after indexer lag; my donations works.
- [ ] NGO can apply, verify email via Clerk, submit, track status, resubmit on reject.
- [ ] NGO unlock reconciles role/application status.
- [ ] NGO can set Pinata keys (Keystore), create a pool (image → prepare → deploy), and see it indexed.
- [ ] NGO can submit a proof and see status; my proofs + reputation works.
- [ ] All error codes + on-chain reverts mapped to user-facing handling.
- [ ] Runs end-to-end against local Anvil + backend; environment switch to testnet is config-only.
- [ ] Security checks: no key custody, secrets in Keystore, no secret logging.

---

## 13. Resolved Decisions
- **Roles:** Donor + NGO; Admin out of scope.
- **Wallet:** external via WalletConnect v2; **re-linking supported**.
- **UI:** Jetpack Compose + Material 3; Clean Architecture + MVVM in presentation.
- **Environment:** build/test on local Anvil now; testnet (Fuji) later via config only.
- **Browsing:** public, pre-auth; actions gated by auth + linked wallet.
- **Updates:** polling (no push).
- **Min SDK 26, portrait phone.**

## 14. Resolved Open Items
1. **NGO document upload** — **RESOLVED:** the client uploads supporting documents **directly to Pinata** using the NGO's keys and passes the resulting `documentsCid` to `POST /ngo/applications`. No backend doc-upload endpoint is used. Documents are optional.
2. **Region vocabulary** — **RESOLVED:** fixed enum, shared by the create-pool form and the Explore filter (exact-match). Canonical list:
   `Global`, `Sub-Saharan Africa`, `North Africa`, `Middle East`, `South Asia`, `Southeast Asia`, `East Asia`, `Central Asia`, `Europe`, `North America`, `Latin America & Caribbean`, `Oceania`.
   The stored/sent `region` string MUST match one of these exactly.
3. **Donation amount UX** — **RESOLVED:** free numeric entry **plus** quick-amount chips (e.g. 10 / 25 / 50 / 100 USDC), with a client-side balance warning when amount > `USDC.balanceOf`.
4. **Account-mismatch policy** — warn-and-block for write actions (FR-2.6).
5. **WalletConnect network-switch UX** — request switch via WalletConnect where the wallet supports it; otherwise show manual guidance.
