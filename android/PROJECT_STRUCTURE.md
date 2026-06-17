# Livana Android — Project Structure

> How the app is organized and why. Read alongside `docs/android_prd.md` (§4) and
> `docs/android_ui_implementation_guide.md`.

## Architecture decision: feature-first + shared `core`

We **package by feature** (like the backend), with a `core/` package holding everything shared.
This is the Now-in-Android pattern and the cleanest scalable choice:

- **Each feature is self-contained** — its screens, ViewModels, UI state, and feature-specific
  use-cases live together. You can see/move a feature as a unit.
- **Shared things live in `core/`** — domain models, repositories, network/chain/auth/storage
  infrastructure, the design system, navigation, and shared stateful UI. Livana's data (pools,
  donations, proofs, reputation, user) is used by many features, so it belongs in one place rather
  than duplicated per feature.
- **Single Gradle module now, modular later** — packages are drawn so any `core/*` or `feature/*`
  package can be promoted to its own Gradle module without moving code around.

Clean Architecture dependency rule holds: `feature (ui)` → `core/model` + `core/data` interfaces;
`core/data` implements repositories using `core/network` + `core/chain` + `core/storage`. UI never
touches DTOs; data never imports UI.

## Tree

```
android/
├─ settings.gradle.kts
├─ build.gradle.kts                 // root plugins
├─ gradle.properties
├─ gradle/libs.versions.toml        // version catalog (verify/upgrade in Android Studio)
└─ app/
   ├─ build.gradle.kts              // deps + per-build-type AppConfig (Anvil now / testnet later)
   ├─ proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ res/
      │  ├─ values/ (strings.xml, themes.xml, font_certs.xml)
      │  ├─ xml/network_security_config.xml   // debug cleartext to 10.0.2.2/localhost only
      │  ├─ font/        // (optional) bundled Plus Jakarta Sans + Fraunces .ttf
      │  ├─ drawable/    // vector icons + state/illustration vectors recreated from mockups
      │  └─ mipmap-*/    // launcher icon (ASK user for final icon)
      └─ java/com/livana/app/
         ├─ LivanaApplication.kt    // @HiltAndroidApp
         ├─ MainActivity.kt         // edge-to-edge, sets LivanaTheme + NavHost
         ├─ di/                     // Hilt modules (Network, Chain, Auth, Storage, Repository)
         ├─ core/
         │  ├─ designsystem/
         │  │  ├─ theme/  (Color, Type, Shape, Dimens, Theme)   // tokens from livana.css
         │  │  └─ component/ (BrandMark + reusable buttons/chips/pills/cards/fields/tabs/sheets…)
         │  ├─ model/     // shared domain entities (Usdc, Region, Pool, Donation, Proof,
         │  │             //   NgoApplication, UserProfile, Reputation, WalletAddress…)
         │  ├─ common/    // Result, DomainError, dispatchers, formatters (usdc/address/time)
         │  ├─ network/   // Retrofit/OkHttp, auth interceptor, Page<T>, error parser
         │  │  ├─ dto/    // wire models (data-layer only)
         │  │  └─ mapper/ // dto -> domain
         │  ├─ chain/     // Web3j provider, TransactionEngine, RevertMapper, WalletConnect client
         │  │  └─ contracts/ // ABI wrappers (IERC20, PoolFactory, FundPool)
         │  ├─ auth/      // Clerk integration, session/token provider
         │  ├─ storage/   // Keystore secrets, Pinata key store, DataStore prefs
         │  ├─ data/
         │  │  └─ repository/ // PoolRepository, DonationRepository, ProofRepository,
         │  │                 //   ReputationRepository, UserRepository, NgoApplicationRepository,
         │  │                 //   StatsRepository (+ impls)
         │  ├─ navigation/ // Destinations + LivanaNavHost + bottom bar
         │  └─ ui/         // shared stateful composables: LoadingSkeletons, EmptyState,
         │                 //   ErrorState, OfflineBanner, TransactionSheet, LivanaBottomSheet
         └─ feature/
            ├─ home/        // HomeScreen, HomeViewModel, HomeUiState  (example pattern — built)
            ├─ explore/     // list + search + FilterSortSheet
            ├─ pooldetail/  // tabbed About/Donations/Proofs
            ├─ donate/      // amount → review → progress → success
            ├─ leaderboard/ // donor + NGO
            ├─ reputation/  // public NGO profile / reputation
            ├─ activity/    // my donations + my proofs
            ├─ profile/
            ├─ settings/
            ├─ wallet/      // connect, link (sign), network/account warning
            ├─ ngoapply/    // become-ngo, application form, email verify, status
            ├─ ngopool/     // create pool form + progress
            ├─ ngoproof/    // submit proof (IPFS CID)
            └─ pinata/      // Pinata key setup
```

Each `feature/<x>/` package contains its `XScreen.kt`, `XViewModel.kt`, `XUiState.kt`, and any
feature-only use-cases. Sub-flows (e.g. donate's 4 steps) are multiple screens in the same package.

## Conventions

- **One ViewModel per screen**, exposing immutable `UiState` as `StateFlow`; one-off events via a
  channel. No business logic in composables.
- **State coverage**: every data screen renders Loading / Content / Empty / Error / Offline
  (see `core/ui` + mockups `33-states.html`, `34-skeletons.html`).
- **Tokens only** for color/type/shape/spacing — from `core/designsystem/theme`.
- **Naming**: `XScreen`, `XViewModel`, `XUiState`; repositories `XRepository` (+ `XRepositoryImpl`);
  use-cases `VerbNounUseCase`; DTOs suffixed `Dto`; mappers `XMapper`/`toDomain()`.
- **Use-case policy (hybrid)**: do **not** add a use case for trivial pass-through reads — those
  ViewModels depend on repositories directly (Now-in-Android style). **Do** introduce a
  `VerbNounUseCase` when there is real orchestration/business logic to encapsulate: the multi-step
  money/trust flows — **donate** (balance/allowance check → approve → donate → poll), **link wallet**
  (challenge → sign → PATCH → reconcile), **create pool** (upload → prepare → deploy → poll),
  **submit proof** (upload → submitProof → poll). Keep that orchestration out of ViewModels.
- **Config-driven environment**: all chain/backend values come from `BuildConfig` (see
  `app/build.gradle.kts`). Anvil now; testnet/mainnet are config-only swaps.

## Status of this skeleton

Built (concrete): build config + manifest + theme (Color/Type/Shape/Dimens/Theme) + BrandMark +
Money/Region models + navigation destinations + a NetworkModule placeholder + the Home feature as a
pattern reference. Everything else is a placeholder package (`.gitkeep`) to be filled per the UI
guide.

## Before first build / before shipping (asks)

- **Fonts**: ✅ done — Plus Jakarta Sans + Fraunces variable `.ttf`s are bundled in `res/font/` and wired in `Type.kt` (no Play Services / certs needed).
- **Logo / launcher icon / brand hero photos**: not finalized — **ask the user** (UI guide §6).
  All logo usage funnels through `BrandMark` for a one-place swap.
- **SDKs**: add Clerk Android SDK + WalletConnect/Reown coordinates to the version catalog & app deps.
- **Dependency versions**: indicative in the catalog — let Android Studio sync/upgrade to stable.
