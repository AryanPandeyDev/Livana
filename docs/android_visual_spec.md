# Livana Android — Visual Design Spec

> Doc 3 of 3. Defines **how every screen looks** and the single design system that unifies them.
> Built to be consumed by a screen-generating design agent (e.g. Google Stitch): there is one shared
> **Style Preamble** (§2) to prepend to every screen prompt, then each screen in §5+ has a ready-to-use,
> self-contained **prompt block**.
>
> Derives from `android_user_flows.md` (behavior) and `android_prd.md` (requirements).

---

## 1. Design Direction

**Feeling:** calm, trustworthy, hopeful, and quietly premium. Livana moves money for humanitarian causes, so the UI should feel transparent and reassuring — never flashy or "crypto-bro." Think of the polish of a modern fintech (Monzo, Revolut, Wise) crossed with the warmth of a giving platform.

**One theme, everywhere.** A single **light, warm** theme across the whole app. No separate dark mode, no theme toggle — one consistent look so the brand reads the same on every screen. (Define colors as tokens so a dark theme *could* be added later, but ship only light.)

**Signature traits:**
- Soft warm off-white canvas with crisp white cards and gentle, low shadows (airy, not heavy).
- A confident emerald/jade primary (growth, trust, generosity) with a warm coral secondary used sparingly as a human, hopeful accent.
- Generous spacing and large rounded corners — comfortable, modern, unhurried.
- Big, elegant numbers for impact stats and amounts (these are the emotional core).
- Full-bleed cause photography with a subtle gradient scrim for legible overlay text.
- Smooth, restrained motion. Nothing bounces aggressively.

---

## 2. Style Preamble (prepend to EVERY screen prompt)

> Copy this block to the top of each screen prompt you send to the design agent, then add the screen-specific section.

```
STYLE — Livana (humanitarian on-chain giving app). Single light theme, no dark mode.
Mobile, Android, portrait. Material 3 foundations with a custom warm palette.

Mood: calm, trustworthy, hopeful, premium-fintech meets charity warmth. Lots of whitespace,
airy layouts, gentle low shadows, large rounded corners.

COLOR TOKENS
- Background (canvas): #FAF8F5  (soft warm off-white)
- Surface (cards/sheets): #FFFFFF
- Surface alt / inputs: #F3F1EC
- Primary (brand, CTAs): #0F766E  (deep jade/emerald)
- Primary pressed: #0B5C56
- Primary container (tint bg): #D7F0EA
- On-primary: #FFFFFF
- Secondary accent (sparing highlights, hope): #F2785C  (warm coral)
- Secondary container: #FCE3DB
- Text primary: #16201D  (near-black, slightly green-warm)
- Text secondary: #5C645F
- Text muted / hint: #94999A
- Border / divider: #E7E3DC
- Success: #1F9D6B   Warning: #E0A458   Error: #D5503F   Info: #2F77B6
- Gradient scrim on photos: top transparent → bottom rgba(10,30,25,0.65)

TYPOGRAPHY
- One family for UI: "Plus Jakarta Sans" (fallback Inter / system sans).
- Optional display family for big hero numbers/amounts: "Fraunces" (elegant serif) — used ONLY for
  large stat figures and donation amounts to add warmth; everything else is the sans.
- Scale: Display 34/40 semibold; H1 26/32 semibold; H2 20/26 semibold; Title 17/24 semibold;
  Body 15/22 regular; Label 13/18 medium; Caption 12/16 regular. Generous line-height, slight
  negative tracking on large headings.

SHAPE & SPACING
- Corner radii: cards 22dp; buttons 16dp (or full pill for primary CTAs); inputs 14dp;
  bottom sheets 28dp top; chips full pill; images 18dp.
- Spacing scale (dp): 4, 8, 12, 16, 20, 24, 32, 40. Screen side padding 20dp. Card padding 20dp.
- Shadows: soft, diffuse, low opacity (e.g. y+8, blur 24, rgba(20,30,25,0.06)). Avoid harsh borders;
  prefer shadow + subtle 1px #E7E3DC divider where needed.

COMPONENTS
- Primary button: filled jade pill, white text, 52dp tall, full-width on forms.
- Secondary button: outlined (1.5px jade) or tonal (primary container bg, jade text).
- Text button: jade text only.
- Cards: white, radius 22, soft shadow, 20dp padding.
- Chips/filters: pill, surface-alt bg unselected, primary container + jade text selected.
- Inputs: surface-alt fill, radius 14, 14dp padding, label above, helper/error below; clear focus ring in jade.
- Progress bar: rounded, jade fill on #E7E3DC track, 8dp tall.
- Bottom nav: white, soft top shadow, 5 items, jade active icon+label, muted inactive.
- Top app bar: transparent/canvas, large title, back chevron when nested, no heavy elevation.
- Iconography: rounded line icons (Material Symbols Rounded / Phosphor), 24dp, text-secondary by default.
- Avatars/identicons: circular; wallet addresses shown truncated (0x12ab…7f9c) in a monospace tint.

IMAGERY
- Cause cover photos are full-bleed with the bottom gradient scrim; overlay text in white.
- Empty/illustrative states use simple, warm line illustrations in jade + coral on canvas.

MOTION
- 200–280ms ease-in-out transitions, gentle fades/slide-ups for sheets, subtle scale on press (0.98).
- Number counters animate up on stats. No aggressive bounce.

ACCESSIBILITY
- WCAG AA contrast; never color-only meaning (pair with icon/label). Touch targets ≥48dp.
- Support large font scaling without breaking layout.
```

---

## 3. Brand & Identity Notes
- **Name:** Livana. **Motif (INTERIM):** a minimal **4-petal bloom** — four jade petals around a coral center — tying to the 🌸 brand. Used small as the logo mark; a single soft organic shape (low-opacity white) is the subtle watermark inside the green hero cards. **The final logo is NOT yet provided by the client — ask before finalizing, and funnel all logo usage through one `BrandMark` component so it swaps in one place.**
- **Logo lockup:** wordmark "Livana" in Plus Jakarta Sans extrabold with the bloom mark to the left.
- **Tone of copy:** warm, plain-spoken, reassuring. "Your gift, fully traceable." Avoid jargon; explain on-chain terms in human language.
- **Trust signals to surface visually:** verified-NGO badge (jade check), "on-chain / verifiable" markers, proof + release status, reputation (SBT) counts.

---

## 4. Global Patterns (referenced by screens)

- **Bottom navigation (authenticated + public shell):** Home · Explore · Leaderboards · Activity · Profile. NGO entries live inside Activity/Profile and appear only when NGO-unlocked.
- **Money formatting:** amounts stored as USDC atomic units; always display as `$1,234.56` (or `1,234.56 USDC`) using the Fraunces display face for large figures. Show the symbol/USDC label consistently.
- **Address formatting:** truncated `0x12ab…7f9c`, tappable to copy, monospace tint.
- **Status pills:** small pill chips — Paused (warning), Verified NGO (success/jade), Proof Pending (info), Released (success), Application states color-coded.
- **Transaction sheet (reusable):** a bottom sheet showing stepper (e.g. "1 Approve → 2 Donate"), each step's state (waiting in wallet / submitting / confirming / done), tx hash (copyable), and a friendly explainer. Used by donate, deploy pool, submit proof, wallet link.
- **State templates (every data screen):** Loading (skeletons), Loaded, Empty (illustration + CTA), Error (illustration + retry), Offline (banner + cached content where possible), Indexer-lag ("Updating…" chip with subtle pulse).
- **Snackbars/toasts:** bottom, concise, with optional action (e.g. "Copied", "Retry").

---
```
SCREEN INDEX (full inventory; detailed prompts follow in §5–§12)

A. Onboarding & Auth
  1. Splash / Launch
  2. Welcome (value prop, pre-auth)
  3. Sign In (Clerk wrapper)

B. Public / Discovery (bottom-nav shell)
  4. Home / Impact dashboard (stats)
  5. Explore Pools (list + search)
  6. Filter & Sort sheet
  7. Pool Detail
  8. Pool — All Donations
  9. Pool — All Proofs
  10. Donor Leaderboard
  11. NGO Leaderboard
  12. NGO Public Profile / Reputation (+ SBT history)

C. Wallet
  13. Connect Wallet (WalletConnect picker)
  14. Link Wallet — sign challenge
  15. Wrong-network / account-mismatch warning

D. Donate
  16. Donate — amount entry
  17. Donate — review & confirm
  18. Donate — transaction progress (approve + donate)
  19. Donate — success / receipt

E. Donor account
  20. Activity — My Donations
  21. Profile / Account
  22. Settings

F. NGO onboarding
  23. Become an NGO (intro)
  24. NGO Application form
  25. Email verification prompt (Clerk)
  26. Application Status (state machine views)

G. NGO operations
  27. Pinata Keys setup
  28. NGO Home / My Pools
  29. Create Pool — form
  30. Create Pool — progress (upload → prepare → deploy)
  31. Submit Proof — form
  32. My Proofs (release status)
  33. My NGO Reputation dashboard

H. System / shared
  34. Transaction status sheet (reusable)
  35. Empty / Error / Offline state templates
  36. Loading skeleton templates
  37. Confirmation & info bottom sheets
```

---

## 5. Onboarding & Auth

### Screen 1 — Splash / Launch
**Purpose:** brand moment while the app resolves session + config; routes to Welcome (no session) or shell (session).
**Layout:** centered Livana petal mark + wordmark on the warm canvas; subtle blossom watermark; thin jade progress indicator near bottom. Minimal.
**States:** loading (default); silent error → fall through to Welcome.
```
SCREEN: Splash. Centered Livana logo (petal mark + "Livana" wordmark in jade) on warm off-white
canvas (#FAF8F5). A faint oversized blossom watermark in primary-container tint bleeds off one corner.
A slim jade indeterminate progress line sits 64dp from the bottom. No text fields, no buttons.
Calm, premium, lots of empty space.
```

### Screen 2 — Welcome (pre-auth value prop)
**Purpose:** first impression for unauthenticated visitors; can explore or sign in.
**Layout:** top hero with warm humanitarian photo + gradient scrim and headline overlay ("Give with proof, not promises."); 2–3 compact trust points with rounded line icons (On-chain transparency · AI-screened NGOs · Verified impact); primary CTA "Explore causes" (enters public shell), secondary text button "Sign in". Small footnote: browse freely, sign in only when you give.
**States:** static.
```
SCREEN: Welcome / onboarding landing. Top 45% is a full-bleed warm photo of humanitarian aid
(hands, community) with the bottom gradient scrim; white overlay headline "Give with proof,
not promises." and a one-line subhead. Below on the canvas: three slim trust rows, each a rounded
line icon in jade + short label ("Every donation on-chain", "NGOs AI-screened & verified",
"See exactly how funds are used"). Bottom: full-width jade pill primary button "Explore causes",
and beneath it a centered jade text button "Sign in". Tiny muted caption: "Browse freely — sign in
only when you're ready to give." Elegant, warm, spacious.
```

### Screen 3 — Sign In (Clerk wrapper)
**Purpose:** host Clerk's email/Google auth; this is the branded frame around the Clerk SDK UI.
**Layout:** top back chevron; Livana logo; H1 "Welcome to Livana"; subhead; the Clerk auth component (email field + continue, "or" divider, Google button) styled to match tokens; legal/footnote links. On success → role resolution → shell.
**States:** idle, submitting (button spinner), error (inline message under field for invalid email / network), canceled (returns to Welcome).
```
SCREEN: Sign in. Back chevron top-left. Centered Livana petal mark, then H1 "Welcome to Livana"
and muted subhead "Sign in or create an account in seconds." A white card contains: an email input
(surface-alt fill, radius 14, label above), a full-width jade pill "Continue" button, a centered
"or" divider with hairlines, and an outlined "Continue with Google" button with the Google glyph.
Below the card, tiny muted text with "Terms" and "Privacy" links in jade. Warm canvas, generous
spacing. Show an inline error style (red helper text) beneath the email field for the error state.
```

---

## 6. Public / Discovery

### Screen 4 — Home / Impact Dashboard (`GET /stats`)
**Purpose:** emotional, trust-building landing for the public shell; platform-wide impact at a glance + entry to explore.
**Layout (scroll):**
- Top bar: Livana wordmark left; profile/avatar or "Sign in" chip right.
- Hero impact card: large Fraunces number "Total donated" (e.g. `$50,000`), with "Total released" beneath; soft jade gradient or tint background; small "on-chain & verifiable" caption.
- Stat row: 3 compact stat tiles — Active pools, Verified NGOs, Total pools (icon + number + label).
- "Featured causes" horizontal carousel of pool cards (cover photo, title, region, progress).
- "Top NGOs" mini list (avatar, name/address, reputation) → link to NGO leaderboard.
- Secondary CTA card: "Become an NGO" (for orgs) — subtle.
**States:** loading skeleton for stats + carousel; error+retry on the stats card; counters animate up.
```
SCREEN: Home / impact dashboard. Top app bar with Livana wordmark left and a circular avatar (or
"Sign in" pill) right, on warm canvas. Below: a large hero card (radius 22, soft jade-tint
background #D7F0EA) with a small label "Total donated on Livana" and a very large elegant serif
(Fraunces) figure "$50,000", and a secondary line "$20,000 released to causes" with a tiny jade
"on-chain · verifiable" pill. Next, a row of three small white stat tiles each with a rounded line
icon, big number, and label: "38 Active pools", "12 Verified NGOs", "42 Total pools". Then a section
header "Featured causes" with a horizontal carousel of pool cards: each card has a full-bleed cover
photo with gradient scrim, title overlay, a region chip, and a thin jade progress bar with
"$5,000 of $10,000". Then "Top NGOs" — a compact list of 3 rows (circular avatar, NGO name or
truncated address, a jade reputation badge with SBT count) and a "See all" text button. Numbers
should feel celebratory but calm. Plenty of whitespace.
```

### Screen 5 — Explore Pools (`GET /pools`)
**Purpose:** browse/search/filter all causes.
**Layout:**
- Top: title "Explore causes"; search field (surface-alt, leading search icon, debounced); filter/sort icon button opening the sheet (Screen 6); active filter chips row (e.g. region selected) below search, removable.
- Vertical list of pool cards: cover photo (left thumb or full-width media card), title, region chip, short description, progress bar with raised/target, donor count if available, Paused pill when paused.
- Infinite scroll with paging spinner; end-of-list subtle "You're all caught up."
**States:** loading skeleton cards; empty ("No causes match your search" + clear filters CTA); error+retry; offline banner.
```
SCREEN: Explore causes (pool list). Header title "Explore causes". A search input (surface-alt fill,
radius 14, leading magnifier icon, placeholder "Search causes…") with a filter icon button to its
right. Under it, a horizontal row of removable filter chips (e.g. a selected "South Asia" region chip
in primary-container with a small x). Then a vertical scroll list of cause cards: each white card,
radius 22, soft shadow, contains a full-width cover photo (radius 18) with a small region chip
overlaid bottom-left on a scrim, then below the image: bold title, one-line muted description, a
thin jade progress bar, and a row "$5,000 raised · goal $10,000" with a small donor-count on the
right. Show a "Paused" warning pill on a paused card. Comfortable vertical rhythm. Include a loading
state with shimmer skeleton cards.
```

### Screen 6 — Filter & Sort sheet
**Purpose:** refine the pool list by region and sort order.
**Layout:** bottom sheet (radius 28 top): title "Filter & sort"; Region section as a wrap of selectable pill chips from the fixed region enum (Global, Sub-Saharan Africa, North Africa, Middle East, South Asia, Southeast Asia, East Asia, Central Asia, Europe, North America, Latin America & Caribbean, Oceania); Sort section as radio rows (Newest, Most raised, Closest to goal); footer with "Reset" text button and full-width jade "Apply" button.
```
SCREEN: Filter & sort bottom sheet. Rounded-top white sheet over a dimmed scrim. Title "Filter & sort"
with a small grabber handle on top. Section label "Region" followed by a wrapped set of pill chips
for: Global, Sub-Saharan Africa, North Africa, Middle East, South Asia, Southeast Asia, East Asia,
Central Asia, Europe, North America, Latin America & Caribbean, Oceania — selected chips use
primary-container background with jade text, unselected use surface-alt. Section label "Sort by" with
three radio rows: "Newest", "Most raised", "Closest to goal". Footer row: a "Reset" jade text button
on the left and a full-width-ish jade pill "Apply" button on the right. Calm and tidy.
```

### Screen 7 — Pool Detail (`GET /pools/{address}`)
**Purpose:** full cause context + the Donate entry point; the key conversion screen.
**Layout (scroll, collapsing header):**
- Collapsing hero: full-bleed cover photo + scrim; back chevron; share icon; title + region chip overlaid.
- Progress block (white card overlapping hero bottom): big raised amount (Fraunces) "of $target", jade progress bar, percent, donor count; secondary "released" figure with a small explainer.
- Primary CTA: sticky bottom full-width jade "Donate" (disabled with "Donations paused" label when paused).
- Creator/NGO row: avatar, NGO name/address, verified badge, reputation (SBT count, total released) → taps to NGO profile.
- **Switchable tabs (About / Donations / Proofs)** below the creator row — tapping a tab swaps the panel beneath; only one shows at a time to avoid a long scroll:
  - About: description (expandable) + escrow trust note.
  - Donations: preview list (donor address, amount, time) + "See all".
  - Proofs: preview list (amount, submitted time, Released/Pending status pill) + "See all".
- Trust footnote: "Funds are held in an on-chain escrow and only released after admin-verified proof."
**States:** loading skeleton (hero + cards); not-found ("This cause is no longer available"); paused variant; error+retry; sections have their own empty states ("No donations yet", "No proofs submitted yet").
```
SCREEN: Pool / cause detail. Collapsing full-bleed cover photo header with gradient scrim, a back
chevron and share icon in white on top, and overlaid at the bottom: large white title and a region
chip. Overlapping the bottom of the hero, a white card (radius 22, soft shadow) shows a big elegant
serif amount "$5,000" with muted "of $10,000 goal" beside/below it, a thin jade progress bar at 50%,
and a row "120 donors · $2,000 released". Below the card: a creator row with a circular NGO avatar,
NGO name with a small jade verified check badge, and a reputation line "5 SBTs · $15,000 released",
chevron to open the NGO profile. Below it, a switchable tab strip "About / Donations / Proofs"
(active tab = jade text + 2dp jade underline) that swaps the panel beneath — only one panel visible
at a time so the user doesn't scroll through all three. About = body text (truncated "Read more")
+ escrow note; Donations = up to 3 rows (identicon, truncated donor address, amount in jade, relative
time) + "See all"; Proofs = up to 3 rows (document icon, claimed amount, submitted date, status pill
("Released" in success green or "Pending" in info blue)) + "See all". A reassuring muted footnote
about on-chain escrow and verified release. A sticky bottom bar holds a full-width jade pill "Donate"
button. Provide a variant where the button is disabled/tonal with the label "Donations paused".
```

### Screen 8 — Pool: All Donations (`GET /donations/pool/{address}`)
**Purpose:** full paginated donor list for a pool (public transparency).
**Layout:** top bar "Donations" + pool title subtitle; list rows (identicon, truncated address, amount in jade, relative time + tappable tx hash); infinite scroll.
**States:** loading, empty ("No donations yet"), error, paging spinner.
```
SCREEN: All donations for a cause. Top app bar with back chevron, title "Donations", and a small
muted subtitle of the cause name. A clean vertical list; each row: circular identicon, truncated
donor address (monospace tint, tap-to-copy), the donation amount right-aligned in jade semibold, and
a second line with relative time and a tiny "tx" link. Subtle dividers. Infinite scroll with a small
jade spinner at the bottom while loading more. Include an empty state with a simple warm illustration
and "No donations yet — be the first to give."
```

### Screen 9 — Pool: All Proofs (`GET /proofs/pool/{address}`)
**Purpose:** full paginated proof-of-impact list for a pool.
**Layout:** top bar "Proof of impact" + pool subtitle; rows: document icon, claimed amount, submitted date, status pill (Released/Pending), tappable to open the IPFS doc (gateway URL) and tx; infinite scroll.
**States:** loading, empty ("No proofs submitted yet"), error.
```
SCREEN: Proof of impact list for a cause. Top app bar back chevron, title "Proof of impact", muted
cause subtitle. Vertical list; each row is a white card-ish row with a document/receipt rounded icon
in primary-container, the claimed amount in semibold, a submitted date line, and a right-aligned
status pill — "Released" (success green with check) or "Pending" (info blue with clock). Tapping a
row hints at opening the stored document. Empty state illustration with "No proofs submitted yet."
Calm, document-archive feel that signals accountability.
```

### Screen 10 — Donor Leaderboard (`GET /donations/leaderboard`)
**Purpose:** celebrate top donors.
**Layout:** title "Top donors"; optional top-3 podium treatment (larger cards with rank, identicon, total); then ranked list rows (rank number, identicon, truncated address, total donated in jade, donation count). Subtle, tasteful — recognition not gamified noise.
**States:** loading, empty, error.
```
SCREEN: Donor leaderboard. Title "Top donors" with a small muted subtitle "Recognizing generous
giving." Optional top-3 highlight: three elevated white cards in a row with rank medals (1 gold-ish
amber, 2 muted, 3 coral), each with a circular identicon, truncated address, and total donated in
the elegant serif. Below, a numbered list of remaining donors: rank, identicon, truncated address,
total donated right-aligned in jade, and a muted "7 donations" line. Refined and warm, not loud.
```

### Screen 11 — NGO Leaderboard (`GET /reputation/leaderboard`)
**Purpose:** discover trustworthy NGOs by reputation.
**Layout:** title "Top NGOs"; ranked list rows: rank, NGO avatar, name/address, verified badge, reputation metrics (SBTs, total released, pool count); tap → NGO profile.
**States:** loading, empty, error.
```
SCREEN: NGO leaderboard. Title "Top NGOs" with subtitle "Ranked by verified impact." Ranked list:
each row has a rank, a circular NGO avatar, the NGO name (or truncated address) with a small jade
verified check, and a metrics line "5 SBTs · $15,000 released · 3 pools" with the headline figure in
jade. Chevron to open the NGO profile. Trust-forward, clean.
```

### Screen 12 — NGO Public Profile / Reputation (`GET /reputation/{addr}` + `/history`)
**Purpose:** an NGO's public trust page.
**Layout (scroll):** header card (NGO avatar, name/address, verified badge, "since" if available); reputation stat tiles (Total SBTs, Total released, Successful pools); "Impact history" = paginated SBT mint list (token id, pool, amount, date, tx); "Their causes" list of pools by this creator (if derivable). 
**States:** loading, empty history ("No verified releases yet"), error.
```
SCREEN: NGO public profile / reputation. Header white card with a large circular NGO avatar, NGO
name (or truncated wallet address, tap-to-copy) with a jade verified check badge. Below, three stat
tiles: "5 SBTs", "$15,000 released", "3 successful pools" — numbers in the elegant serif. Section
"Impact history": a vertical list of soulbound-token mint records, each row with a small medal/seal
icon, the released amount, the pool it came from, a date, and a tiny tx link. Section "Their causes":
horizontal carousel of this NGO's pool cards. A reassuring caption explaining SBTs as non-transferable
proof of verified aid. Polished, credibility-focused.
```

---

## 7. Wallet

### Screen 13 — Connect Wallet (WalletConnect picker)
**Purpose:** establish a WalletConnect session with an external wallet.
**Layout:** sheet or screen: title "Connect your wallet"; short reassuring copy ("Livana never holds your keys or funds"); WalletConnect entry (QR + "Open in wallet app" deep links / wallet list); supported-wallet logos; small "What's a wallet?" info link.
**States:** idle, connecting (spinner), connected (brief success → continue), error/timeout (retry), user dismissed.
```
SCREEN: Connect wallet. A rounded-top white sheet (or full screen) titled "Connect your wallet" with
a calm subhead "Livana never holds your keys or funds — you approve everything in your own wallet."
A WalletConnect area showing a QR code card and a primary jade pill "Open wallet app", plus a small
row of supported wallet logos (e.g. MetaMask, Core). A muted "What is a wallet?" text link with an
info icon. Show a connecting state with a jade spinner and "Waiting for your wallet…". Trustworthy,
not intimidating.
```

### Screen 14 — Link Wallet (sign challenge)
**Purpose:** prove wallet ownership by signing the backend challenge (`/wallet/challenge` → personal_sign → `PATCH /wallet`).
**Layout:** screen/sheet: title "Verify your wallet"; explainer that signing is free and is NOT a transaction; a preview card of the message to sign (the verbatim challenge text, in a monospace block, scrollable); primary "Sign in wallet" button; status area for the steps; the connected address shown truncated.
**States:** requesting challenge (loading), awaiting signature (in-wallet), verifying (PATCH), success (→ resume action), errors mapped: expired/mismatch/invalid-signature/already-linked (each a clear inline message with retry), user-rejected (silent cancel).
```
SCREEN: Verify / link wallet. Title "Verify your wallet" with subhead "Sign a quick message to prove
this wallet is yours. This is free and not a blockchain transaction." Show the connected wallet
address as a truncated pill (0x12ab…7f9c) with a small wallet icon. A white card displays the exact
message to be signed in a monospace, slightly muted block (scrollable), labeled "Message to sign".
A full-width jade pill "Sign in wallet" button. Below, a small status line that cycles through
"Preparing…", "Waiting for signature in your wallet…", "Verifying…". Include an inline error style
(red helper text) for cases like "This wallet is already linked to another account" with a
"Use a different wallet" text button. Reassuring and clear.
```

### Screen 15 — Wrong-network / Account-mismatch warning
**Purpose:** block/guide when the wallet is on the wrong chain or using a different account than linked.
**Layout:** compact bottom sheet: warning icon (amber), title ("Switch network" or "Wrong wallet account"), explanation, primary action ("Switch network" via WalletConnect where supported, or "Open wallet"), secondary "Cancel". For account-mismatch: show linked address vs active address.
```
SCREEN: Network / account warning bottom sheet. Rounded-top white sheet with a centered amber warning
icon in a soft amber circle. Title "Switch to the right network" (variant: "Use your linked wallet
account"). Body explains briefly why. For the account variant, show two small labeled rows: "Linked:
0x12ab…7f9c" and "Active: 0x88cd…21aa" with the mismatch highlighted. Primary jade pill button
"Switch network" (variant "Open wallet"), and a muted "Cancel" text button. Non-alarming but clear.
```

---

## 8. Donate

### Screen 16 — Donate: Amount entry
**Purpose:** choose donation amount.
**Layout:** top: cause mini-header (small cover thumb, title, region). Big amount input centered, Fraunces display, USDC label, with a numeric keypad feel; quick-amount chips (10 / 25 / 50 / 100); available USDC balance line ("Balance: $X") with inline warning if amount > balance; primary "Continue" CTA. Small note: "You'll approve USDC, then confirm your donation — two quick steps."
**States:** idle, invalid (≤0, disabled CTA), insufficient balance (amber warning, still allow continue per spec), wallet-not-linked intercept (route to link first), balance loading.
```
SCREEN: Donate — enter amount. Compact cause header at top (small rounded cover thumbnail, cause
title, region chip). Centered very large editable amount in the elegant serif, e.g. "$50", with a
muted "USDC" label beneath and a blinking jade caret feel. A row of quick-amount pill chips:
"$10", "$25", "$50", "$100". Below, a muted balance line "Your balance: $320.00" that turns amber
with a small warning icon and "Amount exceeds your balance" when too high. A reassuring caption:
"Two quick steps: approve USDC, then confirm." A sticky full-width jade pill "Continue" button,
disabled/greyed when the amount is empty or zero. Clean, focused, confidence-inspiring.
```

### Screen 17 — Donate: Review & Confirm
**Purpose:** final summary before signing.
**Layout:** summary card: cause, amount (large), "to on-chain escrow" note; a 2-step explainer (Approve USDC → Donate); est. network fees note (gas paid in wallet); primary "Confirm & sign" CTA; secondary "Back".
**States:** idle, submitting (transitions to Screen 18).
```
SCREEN: Donate — review. Title "Review your donation". A white summary card: cause name with small
thumb, a large amount in the elegant serif "$50 USDC", and a muted line "Held in on-chain escrow
until verified release." A two-step mini explainer with numbered jade circles: "1. Approve USDC
(allow the pool to receive your USDC)" and "2. Confirm donation". A muted note that network fees are
paid in the user's wallet. Sticky full-width jade pill "Confirm & sign", with a "Back" text button
above. Calm and transparent.
```

### Screen 18 — Donate: Transaction progress (reuses Transaction Sheet)
**Purpose:** show the approve+donate two-tx sequence live.
**Layout:** the reusable Transaction Sheet (Screen 34) configured with two steps "Approve" and "Donate", each cycling waiting-in-wallet → submitting → confirming → done; tx hashes copyable; cancel disabled once submitted.
**States:** step 1 active, step 2 active, success (→ Screen 19), user-rejected (return with retry), reverted (error with explorer/tx hash), approve-done-but-donate-failed (offer "Retry donation" without re-approve).
```
SCREEN: Donate — transaction progress (bottom sheet). Title "Processing your donation". A vertical
two-step stepper: Step 1 "Approve USDC" and Step 2 "Donate", connected by a line. The active step
shows a jade spinner with a status label ("Waiting for signature in your wallet…", "Submitting…",
"Confirming on-chain…") and completed steps show a jade check. Each submitted step reveals a small
copyable tx hash row. A reassuring "Keep this screen open" caption. Provide an error variant where a
step shows a red icon, an explanation, a copyable tx hash, and a "Retry" button (for the
approve-succeeded-donate-failed case the button reads "Retry donation").
```

### Screen 19 — Donate: Success / Receipt
**Purpose:** celebrate and reassure; reflect indexer lag gracefully.
**Layout:** success illustration (warm, blossom motif); "Thank you for your gift" headline; amount + cause summary; "It may take a few seconds to appear in the cause's activity" note with a subtle Updating chip; tx hash links; primary "View cause" / secondary "Share". 
**States:** confirmed-but-not-yet-indexed (default, with Updating hint), indexed (hint clears).
```
SCREEN: Donate — success. Centered warm illustration with the Livana blossom motif in jade + coral,
a big friendly headline "Thank you for your gift" and a line "$50 to Flood Relief Fund". A small
muted "Updating…" chip with a gentle pulse and caption "Your donation is confirmed on-chain and will
appear in the cause's activity shortly." A copyable tx hash row. Primary jade pill "View cause" and a
secondary outlined "Share" button. Joyful but tasteful.
```

---

## 9. Donor Account

### Screen 20 — Activity: My Donations (`GET /donations/me`)
**Purpose:** donor's giving history across pools.
**Layout:** title "Your giving"; optional summary header (total you've donated, number of causes); list rows: cause (resolve pool by address → title/thumb if available, else address), amount, date, tx link; infinite scroll. If NGO, this tab also surfaces NGO sections (see Screen 28 entry).
**States:** wallet-not-linked (prompt link wallet), empty ("You haven't donated yet" + Explore CTA), loading, error.
```
SCREEN: Your giving (my donations). Title "Your giving". A slim summary header card: "You've given"
with a large serif total and a muted "across 4 causes". Then a vertical list; each row: small rounded
cause thumbnail, cause title (or truncated pool address), amount right-aligned in jade, a date line
and tiny tx link. Infinite scroll. Provide a wallet-not-linked state: a friendly card "Link your
wallet to see your giving history" with a jade "Link wallet" button. Provide an empty state with a
warm illustration and "You haven't donated yet" + "Explore causes" button.
```

### Screen 21 — Profile / Account (`GET /users/me`)
**Purpose:** identity, wallet, and entry to settings + NGO actions.
**Layout (scroll):** header (avatar, displayName, email, role badge — Donor or Verified NGO); Wallet card (connected/linked address truncated, copy, network indicator, "Re-link wallet" + "Disconnect"); quick links (Your giving, For NGOs: Become an NGO / My pools / My proofs / Reputation depending on role); Settings entry; Sign out.
**States:** authenticated donor, authenticated NGO (extra NGO section), wallet not linked (Wallet card shows "Link wallet" CTA).
```
SCREEN: Profile / account. Header with a large circular avatar, display name (e.g. "Alice"), muted
email, and a role pill ("Donor" in surface-alt, or "Verified NGO" in jade with a check). A "Wallet"
white card: shows the linked address truncated with a copy icon, a small network chip
("Local / Anvil" now, "Fuji" later), and two actions — a tonal "Re-link wallet" button and a muted
"Disconnect" text button; when no wallet is linked, this card instead shows "Link your wallet" with a
jade button. A list of navigation rows with rounded line icons: "Your giving", and for NGOs
"My pools", "My proofs", "My reputation", plus "Become an NGO" if not yet an NGO. Then "Settings" and
a destructive-styled "Sign out" row at the bottom. Clean account-screen layout.
```

### Screen 22 — Settings
**Purpose:** app preferences + secure key management entry.
**Layout:** grouped list: Account (manage in Clerk link), Wallet (re-link/disconnect), NGO (Pinata keys — NGO only), Network indicator (read-only env), About (version, terms, privacy), Sign out. Toggle: "Clear Pinata keys on sign out".
**States:** static; NGO-only rows hidden for non-NGO.
```
SCREEN: Settings. Grouped settings list on warm canvas with white grouped cards and section labels.
Group "Account": a row "Manage account" (opens Clerk) with chevron. Group "Wallet": "Re-link wallet",
"Disconnect wallet". Group "NGO" (only for NGOs): "Pinata API keys" with a small key icon and a status
hint ("Set" / "Not set"). Group "Preferences": a toggle row "Clear Pinata keys when I sign out".
Group "About": "Network" (read-only value), "Version", "Terms", "Privacy". A final destructive "Sign
out" row in error red. Tidy iOS/Material settings aesthetic, warm palette.
```

---

## 10. NGO Onboarding

### Screen 23 — Become an NGO (intro)
**Purpose:** explain the NGO path and start an application.
**Layout:** hero with headline ("Bring your cause on-chain"); 3-step explainer (Apply & get AI-screened → Admin verification → Create pools & prove impact); requirements note (wallet, official email, registration details); primary "Start application"; if an application already exists, this routes to status instead.
**States:** no application (CTA start), existing application (auto-route to status), wallet-not-linked intercept.
```
SCREEN: Become an NGO (intro). Top hero band with a warm photo + scrim and white headline "Bring
your cause on-chain." Below, a vertical 3-step explainer with numbered jade circles and short copy:
"1. Apply — we AI-screen your organization", "2. Verification — admins approve via secure multi-sig",
"3. Create causes & prove impact to build reputation." A muted requirements card listing what's needed
(a linked wallet, your organization's official email, registration number). A full-width jade pill
"Start application". Encouraging, credible tone.
```

### Screen 24 — NGO Application form (`POST /ngo/applications`)
**Purpose:** collect org details.
**Layout:** form fields: Organization name (≤255), Registration number (≤100), Description (multiline, non-blank), Official email (valid email — with note it must be verified in Clerk), Supporting documents (optional — file picker that uploads directly to the NGO's Pinata, shows resulting CID/filename); the wallet-to-whitelist shown read-only (from profile). Primary "Save & continue" (creates DRAFT). Inline validation.
**States:** idle, validating, field errors, submitting, already-exists (route to status), wallet-not-linked.
```
SCREEN: NGO application form. Title "Your organization". A white form card with stacked inputs:
"Organization name", "Registration number", "Description" (multiline, taller), and "Official email"
with helper text "You'll verify this email in the next step." Then a "Supporting documents (optional)"
uploader tile with a dashed border, upload icon, and "Add documents" — after upload it shows file
chips. A read-only "Wallet to be verified" row showing the linked address truncated with a small lock
icon and helper "Taken from your linked wallet." Sticky full-width jade pill "Save & continue". Show
inline red helper errors under invalid fields. Trustworthy, form-clean, generous spacing.
```

### Screen 25 — Email verification prompt (Clerk)
**Purpose:** ensure officialEmail is verified on Clerk before submit.
**Layout:** card: "Verify your official email"; the email shown; explanation that a code/link will be sent via Clerk; "Verify email" button (launches Clerk add/verify); status (Verified ✓ / Not verified); once verified, "Submit application" enabled.
**States:** not verified, code sent / awaiting, verified, error (`EMAIL_NOT_VERIFIED` on submit routes back here).
```
SCREEN: Verify official email. Title "Verify your official email." A white card shows the email
address with a status pill ("Not verified" amber / "Verified" jade check). Body explains the email
must be verified to confirm you control your organization's inbox. A jade pill "Verify email" button
(launches the verification flow). Once verified, a full-width jade "Submit application" button becomes
enabled below. Calm, single-purpose screen.
```

### Screen 26 — Application Status (state machine)
**Purpose:** show where the application stands; poll while in progress.
**Layout:** a prominent status card with a horizontal stepper (Draft → AI screening → Pending review → Approved/Rejected) and the current state highlighted; state-specific body + illustration:
- DRAFT: "Finish your application" + Continue.
- AI_SCREENING: animated "AI is reviewing…".
- PENDING_REVIEW: "Awaiting admin approval."
- APPROVED: celebratory + "Create your first pool" CTA.
- REJECTED: show rejectionReason in a card + "Start a new application".
**States:** the five above + loading + not-found (→ apply CTA) + error.
```
SCREEN: NGO application status. A horizontal 4-stage stepper across the top: "Draft", "AI screening",
"Pending review", "Approved" (with a branch to "Rejected"), the active stage filled jade and dots for
the rest. Below, a large state card with a matching warm illustration and copy that changes per state:
DRAFT shows "Finish your application" + a jade "Continue" button; AI_SCREENING shows a gentle animated
scanning illustration and "We're reviewing your organization — this can take a little while";
PENDING_REVIEW shows "Awaiting admin approval"; APPROVED shows a celebratory blossom illustration,
"You're a verified NGO!" and a jade "Create your first pool" button; REJECTED shows a soft (non-harsh)
illustration, a card with the rejection reason text, and a "Start a new application" button. Provide
the loading and not-found (apply CTA) variants. Supportive throughout — rejection should feel
constructive, not punishing.
```

---

## 11. NGO Operations

### Screen 27 — Pinata Keys setup
**Purpose:** securely store the NGO's Pinata API key + secret (Keystore) used for IPFS uploads.
**Layout:** explainer ("Your Pinata keys let you upload images and documents to IPFS. Stored securely on your device, never on our servers."); two secure inputs (API key, API secret) with show/hide; "Test & save" (does a lightweight validation or saves and lets first upload validate); status (Set / Not set); link "Where do I find these?".
**States:** empty, entering, validating, saved (success), invalid keys (`PINATA_UNAUTHORIZED` → error), update/clear.
```
SCREEN: Pinata API keys. Title "Connect Pinata (IPFS)". A muted explainer card: "Your Pinata keys
upload your images and documents to IPFS. They're stored securely on this device (Android Keystore)
and never sent to Livana's servers." Two secure inputs with show/hide eye icons: "Pinata API key" and
"Pinata API secret" (surface-alt fill, monospace). A small jade text link "Where do I find these?".
A full-width jade pill "Save keys" button. A status row showing a key icon and "Keys set" (jade) or
"Not set" (muted), plus a "Clear keys" text button in error red when set. Security-reassuring, simple.
```

### Screen 28 — NGO Home / My Pools
**Purpose:** the NGO operations hub (entered from Profile/Activity when NGO-unlocked).
**Layout:** header greeting + reputation snapshot mini-card (SBTs, total released) → full reputation (Screen 33); primary "Create pool" CTA (FAB or top button); "Your pools" list (cover, title, raised/target progress, status pill, proof count) each tapping into a pool management view (donate detail + Submit proof); empty state ("Create your first cause").
**States:** loading, empty (no pools), error.
```
SCREEN: NGO home / my pools. A friendly header "Welcome back" with a compact reputation snapshot card
(jade-tint) showing "5 SBTs · $15,000 released" and a chevron to full reputation. A prominent jade
"Create pool" button (or extended FAB). Section "Your pools": vertical list of the NGO's pool cards,
each with cover photo, title, a jade progress bar with raised/target, a small proof-count chip, and a
status pill (Active/Paused). Tapping opens that pool with a "Submit proof" action. Provide an empty
state with a warm illustration and "Create your first cause" + button. Confident operator dashboard.
```

### Screen 29 — Create Pool: Form (`/pools/upload-image` + `/pools/prepare`)
**Purpose:** collect pool metadata + cover image.
**Layout:** form: Cover image picker (large tappable image tile with crop preview; constraints JPEG/PNG/WebP ≤5MB shown); Title; Description (multiline); Region (dropdown/sheet from the fixed enum); Target amount (USDC, numeric, large, with USDC label); a note that creating a pool requires a wallet transaction; primary "Continue" (kicks off upload→prepare→deploy progress). Inline validation; Pinata-not-set intercept → Screen 27.
**States:** idle, image selected, validating, field errors, image errors (type/size), pinata-not-set, NGO-not-approved intercept.
```
SCREEN: Create pool — form. Title "Create a cause". A large cover-image picker tile at top (radius 18,
dashed border when empty with an image icon + "Add cover photo (JPEG/PNG/WebP, max 5MB)"; once chosen
shows the photo with an edit/replace overlay). Then inputs: "Title", "Description" (multiline),
"Region" as a select row that opens a region picker sheet (the fixed enum list), and "Target amount"
with a large numeric field and a "USDC" suffix. A muted note: "Publishing a cause requires a quick
wallet transaction." Sticky full-width jade pill "Continue". Inline validation errors in red. If
Pinata keys aren't set, show a small inline banner "Connect Pinata first" linking to setup. Clean,
guided, premium form.
```

### Screen 30 — Create Pool: Progress (upload → prepare → deploy)
**Purpose:** show the multi-stage publish sequence.
**Layout:** reuse Transaction Sheet style with three stages: "Uploading image", "Preparing metadata", "Deploying on-chain" (the last is the wallet tx). Each shows spinner→check; the deploy step shows tx hash; success → pool published (with indexer "Publishing…" hint). Errors per stage (Pinata errors, validation, revert, user-rejected) with retry that preserves prior successful stages (don't re-upload).
**States:** uploading, preparing, awaiting signature, deploying/confirming, success (indexer pending), errors per stage.
```
SCREEN: Create pool — progress (bottom sheet). Title "Publishing your cause". A three-step vertical
stepper: "1. Upload cover image", "2. Prepare metadata (IPFS)", "3. Deploy on-chain". Completed steps
get jade checks; the active step shows a jade spinner with a status label; the deploy step adds
"Waiting for signature in your wallet…" then a copyable tx hash. On success, a short success state
with "Your cause is publishing and will appear shortly" and an Updating pulse chip + "View cause"
button. Error variants per step show a red icon, message, and a "Retry" that resumes from the failed
step (earlier successful uploads are preserved). Reassuring, keep-open guidance.
```

### Screen 31 — Submit Proof: Form (`FundPool.submitProof`)
**Purpose:** NGO submits proof-of-impact for a pool they created.
**Layout:** context header (which pool); **Proof IPFS CID field** — the NGO uploads docs to IPFS via their own Pinata separately, then pastes the resulting CID; the app does **not** upload PDFs here — with a "Preview on IPFS gateway" link; claimed amount (USDC, numeric, must be > 0; show pool balance); explainer that funds release after admin verification; primary "Submit proof" (wallet tx). Matches `submitProof(ipfsCid, amount)`.
**States:** idle, invalid/empty CID, amount invalid, submitting (→ progress), reverts (not-creator guarded, empty-cid, zero-amount), user-rejected.
```
SCREEN: Submit proof of impact — form. Title "Submit proof" with a muted subtitle of the pool name.
A "Proof IPFS CID" text field (monospace, prefilled sample "QmProofDocs7xK…aF3") with a small cube
icon and helper "Paste the IPFS CID of your proof folder — upload to IPFS via your Pinata account
first; Livana only records the CID on-chain", plus a "Preview on IPFS gateway" jade link. A "Claimed
amount" numeric field with a "USDC" suffix and a muted helper showing the pool's current balance. A
reassuring info card: "Funds are released only after admins verify your proof on-chain." Sticky
full-width jade pill "Submit proof". Inline validation (amount > 0; CID required). Accountable feel.
```

### Screen 32 — My Proofs (`GET /proofs/me`)
**Purpose:** NGO's proofs across all their pools with release status.
**Layout:** title "Your proofs"; optional filter (All / Pending / Released); list rows: pool name/address, claimed amount, submitted date, status pill (Pending/Released), tap → open doc + tx; infinite scroll.
**States:** wallet-not-linked, empty, loading, error.
```
SCREEN: Your proofs. Title "Your proofs" with a segmented filter (All / Pending / Released). Vertical
list; each row: a document icon in primary-container, the pool name (or truncated address), claimed
amount in semibold, submitted date, and a right-aligned status pill — "Pending" (info blue, clock) or
"Released" (success green, check). Tap opens the document / tx. Provide wallet-not-linked, empty
("No proofs submitted yet"), and loading states. Clear accountability ledger.
```

### Screen 33 — My NGO Reputation dashboard
**Purpose:** the NGO's own reputation + SBT history (self view of Screen 12).
**Layout:** big reputation stat hero (Total SBTs, Total released, successful pools — large serif numbers); "How reputation works" explainer (SBTs minted on each verified release); SBT history list; share/promote option.
**States:** loading, empty ("Your reputation grows with each verified release"), error.
```
SCREEN: My NGO reputation. A hero card (jade-tint) with three large elegant-serif figures and labels:
"5 SBTs", "$15,000 released", "3 successful pools". A short "How reputation works" explainer card:
"Each time admins verify your proof and release funds, you earn a non-transferable Soulbound Token —
permanent, on-chain proof of your impact." Below, an "Impact history" list of SBT mints (seal icon,
amount, pool, date, tx link). A subtle "Share my reputation" outlined button. Empty state:
"Your reputation grows with each verified release." Motivating and credible.
```

---

## 12. System / Shared Components & States

### Screen 34 — Transaction Status Sheet (reusable)
**Purpose:** one consistent component for every on-chain action (donate, deploy pool, submit proof, link sign).
**Layout:** rounded-top sheet; title (action-specific); a 1–3 step vertical stepper; per-step state (waiting-in-wallet, submitting, confirming, done, failed); copyable tx hash per submitted step; reassurance caption; contextual error + Retry; success transitions out.
```
SCREEN: Transaction status sheet (reusable). Rounded-top white sheet over a dim scrim with a grabber
handle. A title that adapts to the action. A vertical stepper of 1–3 steps connected by a line; each
step shows one of: a jade spinner with a status label ("Waiting for signature in your wallet…",
"Submitting…", "Confirming on-chain…"), a jade check (done), or a red alert icon (failed) with a short
reason and a copyable tx hash row. A muted "Keep this screen open while we finish." caption. On
failure, a full-width jade "Retry" button. Consistent, calm, trustworthy across all flows.
```

### Screen 35 — Empty / Error / Offline templates
**Purpose:** consistent non-content states.
```
SCREEN: State templates. Three centered templates on warm canvas, each with a simple warm line
illustration (jade + coral, blossom motif), a short bold headline, a muted one-line explanation, and
an optional jade button:
- EMPTY: e.g. "Nothing here yet" + contextual CTA.
- ERROR: "Something went wrong" + "Try again" jade button.
- OFFLINE: "You're offline" + "Retry", optionally shown as a top banner over cached content.
Friendly, never blaming the user. Consistent vertical composition and spacing.
```

### Screen 36 — Loading Skeleton templates
**Purpose:** shimmer placeholders matching real layouts.
```
SCREEN: Loading skeletons. Shimmer placeholder versions of the key layouts: a stat-card skeleton, a
pool-card list skeleton (image block + 2 text lines + progress bar), a detail-screen skeleton (hero
block + summary card + list rows), and a simple list-row skeleton. Use soft surface-alt blocks with a
gentle left-to-right shimmer. Match the real component shapes and radii.
```

### Screen 37 — Confirmation & info bottom sheets
**Purpose:** reusable confirm/info dialogs (e.g. disconnect wallet, clear keys, "what is a wallet/SBT/escrow").
```
SCREEN: Confirmation / info bottom sheet. Rounded-top white sheet with a title, body text (and for
info variants, a small relevant illustration or icon), and actions: a primary jade pill and a muted
text button. Destructive confirmations (e.g. "Disconnect wallet?", "Clear Pinata keys?") use an error-
red primary action. Info sheets (e.g. "What is a wallet?", "What's a Soulbound Token?", "How escrow
works") are single-action ("Got it"). Concise and friendly.
```

---

## 13. Screen-to-Flow / API Traceability

| # | Screen | Flow (§) | Backend / on-chain |
|---|--------|----------|--------------------|
| 1 | Splash | 3 | `GET /users/me` (if session) |
| 2 | Welcome | 3 | — |
| 3 | Sign In | 4.1 | Clerk SDK → `GET /users/me` |
| 4 | Home/Stats | 6.1 | `GET /stats` |
| 5 | Explore | 6.2 | `GET /pools` |
| 6 | Filter/Sort | 6.2 | query params |
| 7 | Pool Detail | 6.3 | `GET /pools/{address}` |
| 8 | Pool Donations | 6.3 | `GET /donations/pool/{address}` |
| 9 | Pool Proofs | 6.3 | `GET /proofs/pool/{address}` |
| 10 | Donor Leaderboard | 6.4 | `GET /donations/leaderboard` |
| 11 | NGO Leaderboard | 6.4 | `GET /reputation/leaderboard` |
| 12 | NGO Public Profile | 6.5 | `GET /reputation/{addr}` (+ `/history`) |
| 13 | Connect Wallet | 5.4 | WalletConnect |
| 14 | Link Wallet | 5.2 | `GET /wallet/challenge`, `PATCH /wallet` |
| 15 | Network/Account warn | 5.4 | WalletConnect |
| 16 | Donate amount | 7.1 | `USDC.balanceOf` (read) |
| 17 | Donate review | 7.1 | — |
| 18 | Donate progress | 7.1 | `USDC.approve`, `FundPool.donate` |
| 19 | Donate success | 7.1 | poll `/donations/me`, `/pools/{addr}` |
| 20 | My Donations | 7.3 | `GET /donations/me` |
| 21 | Profile | 11 | `GET /users/me` |
| 22 | Settings | 11 | — / Keystore |
| 23 | Become an NGO | 8.1 | — |
| 24 | NGO Application | 8.2 | `POST /ngo/applications` (+ client→Pinata docs) |
| 25 | Email verify | 8.4 | Clerk |
| 26 | Application Status | 8.6 | `GET /ngo/applications/me` (poll) |
| 27 | Pinata Keys | 9.3 | Keystore (used by uploads) |
| 28 | NGO Home/My Pools | 9 | `GET /pools` (filtered by creator) |
| 29 | Create Pool form | 9.1 | `POST /pools/upload-image`, `POST /pools/prepare` |
| 30 | Create Pool progress | 9.1 | `PoolFactory.deployPool` |
| 31 | Submit Proof form | 10.1 | client→Pinata docs |
| 32 | My Proofs | 10.3 | `GET /proofs/me` |
| 33 | My Reputation | 10.3 | `GET /reputation/{myAddr}` (+ `/history`) |
| 34 | Tx Sheet | 2.3 | all on-chain writes |
| 35–37 | State/shared | 2.2 | — |

---

## 14. Notes for the Design-Generation Agent
- **Prepend the §2 Style Preamble to every screen prompt.** It carries the single theme so screens stay consistent.
- Generate **one light theme only** — do not produce dark-mode variants.
- Where a screen lists **states/variants** (loading, empty, error, paused, wallet-not-linked, success), generate each as a separate frame using the same layout.
- Keep **real content** from the prompts (USDC amounts formatted as `$X`, truncated addresses, region names from the fixed enum) so screens look authentic, not lorem-ipsum.
- Favor the established components (jade pill buttons, white radius-22 cards, surface-alt inputs, pill chips, gradient-scrim cause photos, the elegant serif for big numbers).
- Maintain 20dp screen padding, generous spacing, soft low shadows, and large rounded corners throughout.
