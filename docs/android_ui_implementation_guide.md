# Livana Android — UI Implementation Guide (for the build agent)

> **Read this before writing any UI code.** It tells you how to turn the HTML visual references in
> `docs/mockups/` into the Android (Jetpack Compose) UI **without butchering the design**.
>
> The mockups are the **visual source of truth**. This guide makes them implementable and locks down
> the rules that keep the build faithful. When something here conflicts with your instinct, follow this guide.

---

## 0. Source-of-truth hierarchy

When deciding *anything*, consult in this order:

1. **`docs/mockups/*.html` + `docs/mockups/livana.css`** → how it **looks** (layout, color, type, spacing, components). Pixel/visual truth.
2. **`docs/android_visual_spec.md`** → design-system intent and per-screen notes.
3. **`docs/android_user_flows.md`** → how it **behaves** (states, navigation, edge cases).
4. **`docs/android_prd.md`** → what must exist, architecture, non-functional rules.
5. **`docs/android_api_reference.md`** → real data shapes feeding each screen.

If two sources disagree on *appearance*, the mockup wins. If they disagree on *behavior*, the flows doc wins. If still unclear → **ask the user** (see §11).

---

## 1. The golden rules (do not violate)

1. **No invented placeholder images. Ever.** If a screen needs an image asset that has no real data source, **stop and ask the user** to provide it (see §6). The Picsum URLs in the mockups are *placeholders for the reference only* — they must **not** be copied into the app.
2. **One light theme only.** No dark mode, no theme toggle, no dynamic color. Ship exactly the warm light theme in `livana.css`.
3. **Locked tokens.** Every color, font, radius, spacing, and shadow comes from the token set in §3. Never hardcode a one-off hex, sp, or dp that isn't a token. If you need a value that doesn't exist, ask before inventing it.
4. **Match the mockup layout structure**, not just the vibe. Same sections, same order, same spacing rhythm, same component for the same job.
5. **Reuse, don't re-invent.** Build the shared components in §5 once and reuse them across screens, exactly like `livana.css` is shared across the HTML files.
6. **Fraunces is for big numbers only** (amounts, stat figures). Everything else is Plus Jakarta Sans. Never set a heading or body in Fraunces.
7. **Don't draw fake chrome.** The status bar drawn in the mockups represents the real OS status bar — implement it with real Android system bars + insets, not a hand-drawn row (see §7).
8. **When unsure, ask.** A 30-second question beats a butchered screen. See §11 for the decision gates.

---

## 2. Tech baseline (from the PRD)

- **Jetpack Compose + Material 3**, Kotlin, single light `ColorScheme`.
- Clean Architecture; this guide covers the **presentation layer** only.
- **Min SDK 26**, **phone portrait only**.
- Images: **Coil**. Fonts: bundled / Google Fonts (see §4). Icons: hand-built vector paths (see §3.6).
- Edge-to-edge with proper system-bar insets (`WindowCompat.setDecorFitsSystemWindows(window, false)`).

---

## 3. Design system → Compose mapping

Translate `livana.css` `:root` tokens 1:1. Put these in a `Theme.kt` / `Color.kt` / `Type.kt` / `Shape.kt` / `Dimens.kt`. **These hex values are authoritative — copy them exactly.**

### 3.1 Colors

| Token | Hex | Use | Suggested M3 role |
|-------|-----|-----|-------------------|
| canvas | `#F7F4EF` | app background | `background` |
| surface | `#FFFFFF` | cards, sheets | `surface` |
| surface-alt | `#F1EEE8` | inputs, unselected chips, fills | `surfaceVariant` |
| surface-2 | `#FBF9F5` | subtle inset panels | — |
| primary | `#0F766E` | brand, CTAs | `primary` |
| primary-pressed | `#0B5C56` | pressed/hover of primary | — |
| primary-bright | `#13A697` | gradient endpoint | — |
| primary-container | `#D7F0EA` | tints, icon chips, selected chips | `primaryContainer` |
| primary-container-2 | `#E7F6F1` | softer tint | — |
| on-primary | `#FFFFFF` | text/icon on primary | `onPrimary` |
| secondary (coral) | `#F2785C` | sparing accent, coral CTA | `secondary` |
| secondary-pressed | `#E1623F` | pressed coral | — |
| secondary-container | `#FCE3DB` | coral tint chips | `secondaryContainer` |
| secondary-ink | `#B4452A` | text on coral tint | `onSecondaryContainer` |
| gold | `#E0A458` | rank #1, warning accents | — |
| gold-container | `#FBEED6` | paused pill bg | — |
| text | `#16201D` | primary text | `onBackground` / `onSurface` |
| text-secondary | `#566460` | secondary text | `onSurfaceVariant` |
| text-muted | `#929A97` | hints, disabled labels | — |
| border | `#E8E3DA` | borders, progress track | `outlineVariant` |
| hairline | `#EFEBE3` | dividers | — |
| success | `#1F9D6B` | released status | — |
| warning | `#E0A458` | paused | — |
| error | `#D5503F` | errors, destructive, sign out | `error` |
| info | `#2F77B6` | pending status | — |
| focus | `#0F766E` | focus ring | — |

**Status pill colors** (background / text):
- Verified: `primary-container` / `primary`
- Featured/Urgent: `secondary-container` / `secondary-ink`
- Paused: `gold-container` / `#946017`
- Released: `#DBF1E6` / `#16774E`
- Pending: `#E2EEF7` / `#235D86`
- Region (over photo): `rgba(255,255,255,0.93)` / `text`

**Gradients (Compose `Brush`):**
- `gradHero` (green hero cards): linear 145° `#159185 → #0F766E (52%) → #0A5A55`, with a soft white radial highlight from the top-right (`rgba(255,255,255,0.17)` → transparent). Recreate the highlight with a layered `Brush.radialGradient` over the linear, or approximate with a top-right white radial overlay at ~17% alpha.
- `gradPrimary`: linear 135° `#13A697 → #0F766E`
- `gradProgress` (progress fill): linear 90° `#0F766E → #16AE9D`
- `gradCoral`: linear 135° `#F89070 → #EC6A48`
- `scrim` (over cover photos): vertical, transparent (top 30%) → `rgba(8,28,24,0.66)` (bottom)

> The faint top "ambient glow" (`.screen::before` in the CSS) is decorative depth. Optional to reproduce; if you do, keep it ≤7% alpha and behind content. Do **not** add the blossom background art that was removed.

### 3.2 Typography

Two families only (+ mono for addresses):
- **Plus Jakarta Sans** — all UI text. Weights: 400, 500, 600, 700, 800.
- **Fraunces** — display serif, **big numbers/amounts only** (e.g. `$50,000`, donation amount, stat figures). Weights 500–700, roman (never italic).
- **Monospace** (system mono) — wallet addresses, tx hashes, IPFS CIDs only.

Bundle the fonts (don't rely on the device having them). Use `androidx.compose.ui.text.googlefonts` (downloadable fonts) or ship the `.ttf`s in `res/font`. Never silently fall back to the system sans — that changes the whole look.

**Type scale** (size / line-height, all Plus Jakarta Sans unless noted):

| Style | Size/LH | Weight | Notes |
|-------|---------|--------|-------|
| Display (Fraunces) | 34 / 40 | 600 | hero figures; scales up to ~58 for the donate amount |
| H1 | 26 / 32 | 800 | screen titles ("Explore causes") |
| H2 | 20 / 26 | 700/800 | section / card titles |
| Title | 17 / 24 | 700 | list item titles, app-bar titles |
| Body | 15 / 22 | 400/500 | default |
| Label | 13 / 18 | 600 | field labels, chips |
| Caption | 12 / 16 | 400/500 | helper, muted meta |

Slight negative letter-spacing (~ -0.01 to -0.02em) on large headings and Fraunces figures.

### 3.3 Shape / radii

| Token | dp | Apply to |
|-------|----|---------| 
| r-card | 22 | cards, sheets body |
| r-img | 18 | cover images, image tiles |
| r-input | 14 | text fields, small controls |
| r-btn | 16 | (buttons are mostly full pill — `RoundedCornerShape(percent=50)` / `CircleShape` height) |
| sheet top | 28 | bottom-sheet top corners only |
| chips/pills | full pill | `RoundedCornerShape(50)` |
| icon chips | 12–14 | the tinted square icon containers |

### 3.4 Spacing

4-based scale (dp): **4, 8, 12, 16, 20, 24, 32, 40**. Screen side padding = **20**. Card padding = **20** (compact cards 14–16). Never use off-scale values like 17 or 23.

### 3.5 Elevation / shadows

Compose elevation won't match the CSS shadows exactly; replicate intent:
- `shadow-sm`: subtle (cards in lists) — ~3–4 dp soft.
- `shadow`: standard card — ~8 dp soft, very low alpha.
- `shadow-lg`: sheets, the phone frame — larger.
- `shadow-jade` / `shadow-coral`: **colored** shadows under the primary/coral pill buttons. Use a colored `Modifier.shadow(elevation, ambientColor=primary, spotColor=primary)` (API 28+) or a custom soft shadow. Keep them soft and low-alpha; this gives buttons their "lift."

Prefer shadow + the `hairline` divider over hard borders.

### 3.6 Iconography

- **One icon style**: hand-built rounded line icons, `viewBox 0 0 24 24`, stroke = `currentColor`, stroke-width ~1.8, round caps/joins. Recreate the exact SVG paths from the mockups as Compose `ImageVector`s (or vector drawables). The mockups contain the canonical paths — copy them.
- **No icon libraries mixed in**, **no emoji as icons**, no Material filled icons substituted for the line set.
- Status-bar glyphs (signal/wifi/battery) are **not app icons** — they belong to the real OS status bar; don't recreate them as content.

---

## 4. Fonts — setup checklist

- [ ] Plus Jakarta Sans (400/500/600/700/800) available app-wide as the default `FontFamily`.
- [ ] Fraunces (500/600/700) available as a `displayFontFamily`, used only in the number/amount text styles.
- [ ] Monospace family wired for `Addr`/`TxHash`/`Cid` text.
- [ ] No screen renders in a system fallback font. Verify on a clean device/emulator.

---

## 5. Component catalog → composables

Build these as reusable composables (one place, reused everywhere — mirror how `livana.css` classes are shared). Each must implement **all interaction states** noted.

| Mockup class | Composable | States / notes |
|--------------|-----------|----------------|
| `.btn--primary` | `LivanaPrimaryButton` | default / pressed (`#0B5C56`) / focused (2dp jade ring, offset, **instant** no fade) / disabled (alpha .5, no shadow). 54dp tall, full-width on forms, jade colored shadow. |
| `.btn--coral` | `LivanaCoralButton` | same states, coral + coral shadow. Use sparingly. |
| `.btn--tonal` | `LivanaTonalButton` | primary-container bg, jade text. |
| `.btn--outline` | `LivanaOutlineButton` | 1.5dp jade border, jade text, hover→container. |
| `.btn--ghost` | `LivanaTextButton` | jade text only. |
| `.chip` / `.chip--on` | `LivanaChip` | unselected (surface-alt) / selected (primary-container + jade). Filter + segmented uses. |
| `.pill--*` | `StatusPill(kind)` | one composable, variants: Verified, Featured, Paused, Released, Pending, Region. Always pair color with an icon/label (never color-only). |
| `.iconbtn` / `--glass` | `IconButtonLivana` | 42dp rounded-square; `--glass` = translucent white over photos. |
| `.card` / `--flat` / `--media` | `LivanaCard` | white, r-card, shadow vs shadow-sm; `--media` clips an image at top. |
| `.icochip--jade/coral/gold/info` | `IconChip(tint)` | tinted rounded-square icon container. |
| `.field` (+ textarea, suffix) | `LivanaTextField` | see §5.1 input states. |
| `.progress` / `__fill` | `LivanaProgress` | 8–9dp track (`border`), fill = `gradProgress`, rounded. |
| `.bottomnav` / `.navitem` | `LivanaBottomBar` | 5 items (Home/Explore/Boards/Activity/Profile), jade active. Use `Scaffold` bottomBar. |
| `.dock` | sticky bottom action | `Scaffold` bottomBar or a bottom-anchored surface with top hairline + shadow. |
| bottom sheet pattern | `LivanaBottomSheet` | r-28 top, 40×4 grabber, dim scrim `rgba(8,28,24,0.45)`. Used by filter, connect/link wallet, tx progress, confirm, network warning. |
| `.pdtabs` (pool detail) | `LivanaTabs` | switchable tabs (About/Donations/Proofs); active = jade text + 2dp jade underline. |
| transaction stepper | `TxStepper` | step states: done (jade check) / active (jade spinner) / pending (hollow). Copyable tx-hash row. |
| `.avatar` | `Avatar` | circular; donor = identicon from address (see §6); image = Coil. |
| `Addr` | `AddressText` | mono, truncated `0x12ab…7f9c`, tap-to-copy. |

### 5.1 Input field states (must all be handled)

- Keep **border-width constant** across states (state changes go to bg/outline/border-color, never width — avoids layout shift).
- Focus = `outline` style ring in `focus` color, **appears instantly**.
- Field height == adjacent button height baseline (≥44dp).
- Reserve helper/error space (don't let layout jump when an error appears).
- Disabled = reduced alpha **+** not-clickable **+** semantics disabled (not opacity alone).

### 5.2 Money / address / amount formatting

- All amounts arrive as **USDC atomic units (6 decimals)** integers. Display `value / 1_000_000` as `$X,XXX.XX` (or `… USDC`). Use `BigInteger`/`BigDecimal`, never floats. Big figures use the Fraunces style.
- Addresses/tx/CID: mono, truncated for addresses/tx (`0x12ab…7f9c`), tap-to-copy.

### 5.3 Motion

- Durations 200–280ms, ease-in-out. Subtle press scale (~0.98) on buttons.
- **No `transition: all` equivalents** — animate specific properties. No uniform across-the-board scale-on-press for non-buttons. No bouncy/overshoot easing on UI state changes.
- **Respect "reduce motion"**: disable spinners/shimmer/pulse/counters when the system setting is on. Provide static fallbacks.
- Number counters may animate up on stats (disable under reduce-motion).

---

## 6. Imagery & assets policy (CRITICAL — read fully)

**Rule: never ship invented or placeholder imagery. If real content/asset isn't available, ASK the user.**

Classify every image slot:

1. **Data-driven images (implement against the API, no placeholder needed):**
   - **Pool cover images** → `coverImageCid` → load `"<ipfs-gateway>/<cid>"` via Coil. While loading: a **solid token-colored placeholder block** (e.g. `surface-alt` or the matching `gradHero`/`gradCoral`), **not** a stock photo. If `coverImageCid` is null/unreachable: show the gradient block + a small line-icon, never a random image.
   - **Donor avatars** → generate a **deterministic identicon from the wallet address** (e.g. blockies/jazzicon-style). This is derived data, not a placeholder — allowed. Match the circular avatar look from the mockups.

2. **Brand assets that have NO data source → ASK THE USER before building the screen:**
   - **Welcome hero photo** (`01-welcome`) and **Become-an-NGO hero photo** (`21-become-ngo`) — these are full-bleed brand photographs. **Do not** use Picsum/stock. Ask the user to supply the image(s) or confirm a treatment (e.g. a brand gradient instead of a photo).
   - **NGO avatars/logos** — the backend has no NGO avatar field. Decide with the user: generated identicon from the NGO wallet, initials monogram, or a user-provided asset. **Ask.**
   - **The Livana logo / blossom mark** — **not finalized.** The bloom mark in the mockups is interim. Before shipping, **ask the user for the final logo** (vector). Until then, isolate it in one `BrandMark` composable so it can be swapped in one place.
   - **App icon / launcher icon / splash art** — ask the user; do not auto-generate.

3. **Vector illustrations that ARE part of the design (reproduce, don't ask):**
   - Empty/error/offline state illustrations, the success-screen badge, the "scanning" application-status illustration, decorative line icons — these are simple SVGs defined in the mockups. Recreate them as vector drawables. They are design, not placeholders.

**When you hit an unclassified image need:** stop and ask. Use this template:
> "Screen `NN-name` needs an image for `<slot>`. There's no data source for it. Options: (a) you provide the asset, (b) I use a brand-gradient/identicon treatment, (c) other. Which do you want?"

Asset handling specs: cover images `ContentScale.Crop`; always apply the `scrim` under overlaid white text on photos; provide `contentDescription` (or null + decorative) appropriately; cache via Coil.

---

## 7. Status bar, insets, and chrome

- The drawn status bar in each mockup (`.statusbar` with `9:41` + signal/wifi/battery) **represents the real OS status bar**. Implement edge-to-edge and let Android draw it. **Do not** build a fake status-bar row.
- Set **status bar icon color** per screen: dark icons on light screens; **light icons** over photo/gradient heroes (Welcome, Pool detail hero, Become-an-NGO). Use `WindowInsetsControllerCompat.isAppearanceLightStatusBars`.
- Respect insets: top content padding = status bar inset; bottom nav / docks padded by navigation-bar inset. Heroes bleed under the status bar (content draws edge-to-edge) but interactive content respects insets.
- Bottom nav and sticky "dock" buttons use `Scaffold`; don't let content hide behind them — pad the scroll content by their height.

---

## 8. Per-screen workflow

For each screen:

1. **Open the matching mockup file** (table in §12) in a browser to see it rendered, and read its HTML for exact structure/spacing.
2. **Identify the layout skeleton**: app bar/hero → scroll content sections → bottom nav/dock. Reproduce that order and the spacing rhythm (§3.4).
3. **Map every element to a §5 composable.** If a needed composable doesn't exist yet, build it once (reusable), then use it.
4. **Wire real data** from the API (`android_api_reference.md`) and the relevant flow (`android_user_flows.md`) — including loading / empty / error / offline / indexer-lag states (the mockups show the "loaded" state; you must also build the others per the flows doc and the state templates in `33-states.html` / `34-skeletons.html`).
5. **Handle images per §6** (ask if needed).
6. **Verify against §10 checklist** before calling it done.

Do not batch-generate all screens blind. Build the shared theme + component library first, then screens.

---

## 9. State coverage (don't ship only the happy path)

Every data screen needs: **Loading** (skeletons — see `34-skeletons.html`), **Loaded**, **Empty** (see `33-states.html`), **Error + retry**, **Offline**. Lists also need **paging** and **end-of-list**. On-chain actions need the **transaction sheet** states (waiting-in-wallet / submitting / confirming / done / failed) and the post-action **indexer-lag "Updating…"** hint. These are specified in `android_user_flows.md` §2 and §12 — implement them; the mockups mostly show the loaded state.

---

## 10. Per-screen verification checklist

Before a screen is "done":

- [ ] Layout structure, section order, and spacing match the mockup.
- [ ] All colors/type/radii/shadows come from tokens (no stray hex/sp/dp).
- [ ] Fraunces used only for big numbers; everything else Plus Jakarta Sans; no system-font fallback.
- [ ] Every interactive element has default/pressed/focus/disabled (+ loading/error where relevant).
- [ ] No placeholder/stock images. Data images load via Coil with token-colored fallbacks; brand assets confirmed with the user.
- [ ] Real status bar with correct icon contrast; insets respected; nothing hidden behind nav/dock.
- [ ] Loading/empty/error/offline states implemented (not just loaded).
- [ ] Money formatted from atomic units; addresses truncated + copyable.
- [ ] Reduce-motion respected; transitions are specific (no `transition:all` equivalent), durations 200–280ms.
- [ ] Touch targets ≥48dp; content descriptions set; text scales without breaking; contrast AA.
- [ ] Single light theme (no dark variant introduced).

---

## 11. When to STOP and ask the user

Ask before proceeding if any of these:

- A screen needs an **image with no data source** (brand photo, NGO avatar, logo, app icon) → §6.
- The **final logo** hasn't been provided and you're about to finalize branding.
- A value/treatment is **not defined in the mockups or tokens** and you'd have to invent it (a new color, a new font, a new spacing, a new component pattern).
- The mockup and the data model **can't both be satisfied** (e.g. a field shown that the API doesn't return).
- A **new screen/state** is implied by the flows but has **no mockup** — confirm the visual treatment.
- Anything that would **deviate** from the mockups (different layout, added dark mode, different nav structure).

Keep questions specific and offer options + a recommendation.

---

## 12. Mockup → screen → flow/API map

> File numbers are the mockup filenames in `docs/mockups/`. "Spec #" is the label in `android_visual_spec.md`.

| Mockup file | Screen | Flow § | Primary API / on-chain |
|-------------|--------|--------|------------------------|
| `01-welcome.html` | Welcome (pre-auth) | 3, 4 | — |
| `02-home.html` | Home / impact dashboard | 6.1 | `GET /stats` |
| `03-explore.html` | Explore pools | 6.2 | `GET /pools` |
| `06-filter-sort.html` | Filter & sort sheet | 6.2 | query params (region enum) |
| `04-pool-detail.html` | Pool detail (tabbed) | 6.3 | `GET /pools/{address}` |
| `07-pool-donations.html` | Pool donations | 6.3 | `GET /donations/pool/{addr}` |
| `08-pool-proofs.html` | Pool proofs | 6.3 | `GET /proofs/pool/{addr}` |
| `09-donor-leaderboard.html` | Donor leaderboard | 6.4 | `GET /donations/leaderboard` |
| `10-ngo-leaderboard.html` | NGO leaderboard | 6.4 | `GET /reputation/leaderboard` |
| `11-ngo-profile.html` | NGO public profile | 6.5 | `GET /reputation/{addr}` (+ `/history`) |
| `12-connect-wallet.html` | Connect wallet (sheet) | 5.4 | WalletConnect |
| `13-link-wallet.html` | Link wallet (sign) | 5.2 | `GET /users/me/wallet/challenge`, `PATCH /users/me/wallet` |
| `14-network-warning.html` | Network/account warning | 5.4 | WalletConnect |
| `05-donate.html` | Donate — amount | 7.1 | `USDC.balanceOf` |
| `15-donate-review.html` | Donate — review | 7.1 | — |
| `16-donate-progress.html` | Donate — progress | 7.1 | `USDC.approve`, `FundPool.donate` |
| `17-donate-success.html` | Donate — success | 7.1 | poll `/donations/me` |
| `18-my-donations.html` | My donations | 7.3 | `GET /donations/me` |
| `19-profile.html` | Profile | 11 | `GET /users/me` |
| `20-settings.html` | Settings | 11 | — / Keystore |
| `21-become-ngo.html` | Become an NGO | 8.1 | — |
| `22-ngo-application.html` | NGO application form | 8.2 | `POST /ngo/applications` (+ client→Pinata docs CID) |
| `23-email-verify.html` | Email verification | 8.4 | Clerk |
| `24-application-status.html` | Application status | 8.6 | `GET /ngo/applications/me` (poll) |
| `25-pinata-keys.html` | Pinata keys | 9.3 | Keystore |
| `26-ngo-home.html` | NGO home / my pools | 9 | `GET /pools` (by creator) |
| `27-create-pool-form.html` | Create pool — form | 9.1 | `POST /pools/upload-image`, `POST /pools/prepare` |
| `28-create-pool-progress.html` | Create pool — progress | 9.1 | `PoolFactory.deployPool` |
| `29-submit-proof.html` | Submit proof (IPFS CID) | 10.1 | `FundPool.submitProof(ipfsCid, amount)` |
| `30-my-proofs.html` | My proofs | 10.3 | `GET /proofs/me` |
| `31-ngo-reputation.html` | My reputation | 10.3 | `GET /reputation/{myAddr}` (+ `/history`) |
| `32-transaction-sheet.html` | Transaction sheet (reusable) | 2.3 | all on-chain writes |
| `33-states.html` | Empty / error / offline templates | 2.2 | — |
| `34-skeletons.html` | Loading skeletons | 2.2 | — |
| `35-confirm-sheets.html` | Confirmation sheet | — | — |

---

## 13. Reference: the region enum (exact strings)

Pool `region` is exact-match; the create-pool picker and the explore filter must use exactly these:

`Global`, `Sub-Saharan Africa`, `North Africa`, `Middle East`, `South Asia`, `Southeast Asia`, `East Asia`, `Central Asia`, `Europe`, `North America`, `Latin America & Caribbean`, `Oceania`.

---

## 14. TL;DR

Build the **theme + shared components from the tokens first**, then implement screens against the mockups one by one, wiring real data and all states. **Never invent images — ask.** Keep one light theme, tokens only, Plus Jakarta Sans + Fraunces (numbers), real OS status bar, and faithful layout. When in doubt, ask the user.
