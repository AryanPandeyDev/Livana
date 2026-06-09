# Livana — Smart Contracts PRD

> Product Requirements Document for the on-chain layer of Livana. This document defines **what** each contract does, **why** each design decision was made, and **how** contracts interact — based on industry-standard patterns from production systems.

---

## Research Foundations

The following industry patterns were studied before designing this system:

| Pattern | Source | Key Takeaway |
|---|---|---|
| Multi-sig governance | **Gnosis Safe** — linked-list owner storage, `execTransaction` with packed signatures, on-chain `confirmTransaction` fallback | We adopt the on-chain confirmation pattern (propose → confirm → auto-execute) for transparency. We skip Safe's proxy/module complexity — overkill for 3 fixed signers. |
| Soulbound Tokens | **EIP-5192** (Minimal Soulbound NFTs) + **OpenZeppelin v5** `_update` hook | EIP-5192 defines `locked(tokenId)` + EIP-165 interface ID `0xb45a3c0e`. In OZ v5, `_beforeTokenTransfer` was removed — the `_update` function is now the single hook for mint/transfer/burn. Override it to block transfers between non-zero addresses. |
| Access Control | **OpenZeppelin AccessControl** vs **Ownable2Step** | `AccessControl` is better for multi-role systems (RBAC). However, since our privileged actions all go through multi-sig, we use simple `msg.sender` checks against the multi-sig address — simpler and sufficient. |
| Token Safety | **OpenZeppelin SafeERC20** | Many ERC20 tokens (including USDT) don't return `bool` from `transfer()`. `SafeERC20` wraps calls to handle missing return values. **Must use for all USDC interactions** even though USDC is well-behaved — defense in depth. |
| IPFS CID Storage | **`bytes32` vs `string`** on-chain | `bytes32` is cheaper (~20K gas for one slot) but requires stripping the multihash prefix off-chain. `string` costs more but is simpler. **Decision: store CID as `string`** — proof submissions are infrequent (low gas impact), and it simplifies frontend integration. |
| Events vs Storage | **Events cost ~375 gas vs SSTORE ~20K gas** | Smart contracts **cannot read events** — only indexers/frontends can. Store on-chain only what the contract needs to read. Emit events for everything else. |
| Factory Pattern | **CREATE vs CREATE2** | `CREATE` (Solidity `new` keyword) is simpler and sufficient. We don't need deterministic addresses or cross-chain parity. Use `CREATE` for pool deployment. |
| Escrow Pattern | Production escrow contracts | State machine with enum lifecycle. `ReentrancyGuard` on all fund-moving functions. Checks-Effects-Interactions (CEI) pattern. No `selfdestruct`, no `delegatecall`. |
| Reentrancy | **OpenZeppelin ReentrancyGuard** | Apply `nonReentrant` to `donate()` and `releaseFunds()`. Even with CEI pattern, belt-and-suspenders approach is industry standard. |
| Pausable | **OpenZeppelin Pausable** | Use `whenNotPaused` on `donate()`. Emergency mechanism controlled by multi-sig. Does not affect existing funds or releases. |
| Upgradability | **UUPS Proxy vs Immutable** | Immutable contracts are the gold standard when trustlessness is priority #1. Our contracts handle donor funds — **we choose immutable** (no proxy, no upgrade mechanism). "Code is law" maximizes donor trust. |

---

## System Overview

Livana's on-chain system consists of **4 contracts**:

```
┌──────────────────────────────────────────────────────────┐
│                    MultiSigAdmin                          │
│  2-of-3 threshold governance for all critical actions     │
└────────────┬────────────────────────────┬────────────────┘
             │ calls                       │ calls
             ▼                             ▼
┌────────────────────────┐    ┌──────────────────────────┐
│      PoolFactory       │    │      LivanaSBT         │
│  NGO registry          │    │  Soulbound reputation    │
│  Pool deployer         │    │  ERC-721 (non-transfer)  │
└────────────┬───────────┘    └──────────────────────────┘
             │ deploys                     ▲ mints after
             ▼                             │ successful release
┌────────────────────────┐─────────────────┘
│       FundPool         │
│  Per-cause escrow      │
│  IPFS proof tracking   │
│  Multi-sig release     │
└────────────────────────┘
```

### Trust Model: Two Gates

| Step | What Happens | Gate |
|---|---|---|
| **NGO Onboarding** | Apply → AI pre-screening → 2-of-3 admins approve → wallet whitelisted on-chain | Multi-sig gate |
| **Pool Creation** | Verified NGO creates a pool — goes live immediately | No gate (NGO already vetted) |
| **Donations** | Donors send USDC to pool escrow contract | Open |
| **Proof Submission** | NGO uploads docs to IPFS → submits CID + amount on-chain | No gate |
| **Fund Release** | 2-of-3 admins review IPFS docs and approve | Multi-sig gate |
| **Reputation** | SBT minted to NGO after successful release | Automatic |

> [!IMPORTANT]
> No EOA (externally owned account) has direct admin access to any contract. Every privileged action goes through `MultiSigAdmin` and requires 2-of-3 signer consensus.

---

## Contract 1: `MultiSigAdmin.sol`

### Purpose
Lightweight 2-of-3 multi-signature governance. All critical platform actions are routed through this contract.

### Why Custom Over Gnosis Safe?
Gnosis Safe is a full-featured modular wallet with proxy upgrades, modules, guards, fallback handlers, and linked-list owner management. We need **none of that**. Our requirements are:
- Fixed 3 signers (no signer rotation in MVP)
- Fixed threshold of 2
- Propose → confirm → execute

A custom ~150-line contract is simpler, cheaper to deploy, easier to audit, and has a smaller attack surface.

### How It Works

```
Signer A calls submitTransaction(targetContract, encodedFunctionCall)
    → Transaction stored with txId
    → Auto-confirms for Signer A (count = 1)

Signer B calls confirmTransaction(txId)
    → Count reaches 2 (threshold met)
    → Auto-executes: MultiSigAdmin.call(target, data)
    → Target contract sees msg.sender = MultiSigAdmin address
```

### State

| Variable | Type | Description |
|---|---|---|
| `signers` | `address[3]` | Fixed 3 signer addresses, set in constructor |
| `isSigner` | `mapping(address => bool)` | Fast lookup for signer validation |
| `THRESHOLD` | `uint256 constant = 2` | Required confirmations |
| `transactionCount` | `uint256` | Nonce / proposal counter |
| `transactions` | `mapping(uint256 => Transaction)` | txId → Transaction struct |
| `confirmed` | `mapping(uint256 => mapping(address => bool))` | txId → signer → hasConfirmed |

**Transaction struct:**
```
{ address to, bytes data, bool executed, uint256 confirmationCount }
```

### Functions

| Function | Who Can Call | What It Does |
|---|---|---|
| `submitTransaction(to, data)` | Any signer | Creates proposal, auto-confirms for submitter. Returns `txId`. |
| `confirmTransaction(txId)` | Any signer | Adds confirmation. Auto-executes if threshold met. |
| `revokeConfirmation(txId)` | Any signer | Removes own confirmation (only if not yet executed). |
| `executeTransaction(txId)` | Any signer | Manual execute fallback if auto-execute didn't trigger. |
| `getTransaction(txId)` | Anyone (view) | Returns transaction details. |

### Events
- `TransactionSubmitted(txId, submitter, to, data)`
- `TransactionConfirmed(txId, signer)`
- `ConfirmationRevoked(txId, signer)`
- `TransactionExecuted(txId)`

### Security
- Nonce-based `txId` prevents replay attacks
- `confirmed` mapping prevents double-confirmation
- `executed` flag prevents double-execution
- Uses `call()` for execution (supports any function signature)
- No `delegatecall`, no ETH handling, no `selfdestruct`

---

## Contract 2: `PoolFactory.sol`

### Purpose
Two responsibilities:
1. **NGO Registry** — Global whitelist of verified NGO wallets
2. **Pool Deployer** — Deploys `FundPool` escrow contracts for verified NGOs

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Only MultiSigAdmin can modify NGO whitelist** | 2-of-3 consensus required to verify or revoke an NGO |
| **Verified NGOs can deploy pools directly** | No admin bottleneck. NGO is already vetted at onboarding. |
| **NGO verification is global** | Verified once, assignable to any pool. Revocation blocks ALL pools instantly. |
| **No metadata on-chain** | Pool names, descriptions, regions stored off-chain. On-chain = only what contract logic needs. |
| **Pool ID = contract address** | No separate ID scheme. Deployed address is the unique identifier. |
| **Factory deploys LivanaSBT** | Solves circular dependency (SBT needs factory, factory needs SBT). Factory creates SBT in constructor → no initialization step needed. |
| **Cap validation** | `maxPerClaim ≤ maxPerNGOPerDay ≤ maxPerNGOPool` enforced at deployment. |

### State

| Variable | Type | Description |
|---|---|---|
| `multiSigAdmin` | `address immutable` | MultiSigAdmin contract address |
| `usdc` | `IERC20 immutable` | USDC token contract |
| `sbt` | `LivanaSBT immutable` | SBT contract (deployed in constructor) |
| `verifiedNGOs` | `mapping(address => bool)` | Global NGO whitelist |
| `isPool` | `mapping(address => bool)` | Tracks deployed pool addresses |
| `poolCount` | `uint256` | Total pools deployed |

### Functions

| Function | Who Can Call | What It Does |
|---|---|---|
| `addVerifiedNGO(ngo)` | MultiSigAdmin only | Whitelist an NGO wallet on-chain |
| `revokeNGO(ngo)` | MultiSigAdmin only | Remove NGO from whitelist. Blocks future releases in ALL pools. |
| `isVerified(ngo)` | Anyone (view) | Check if address is a verified NGO. Called by FundPool at release-time. |
| `isPool(addr)` | Anyone (view) | Check if address is a factory-deployed pool. Called by SBT for mint access control. |
| `deployPool(maxPerClaim, maxPerNGOPerDay, maxPerNGOPool)` | Verified NGOs only | Deploy a new FundPool with immutable spending caps. Returns pool address. |

### Events
- `NGOApproved(ngo)`
- `NGORevoked(ngo)`
- `PoolDeployed(poolAddress, creator, poolIndex, maxPerClaim, maxPerNGOPerDay, maxPerNGOPool)`

### Critical Behavior: Revocation
When an NGO is revoked via `revokeNGO()`:
- Takes effect **immediately** across ALL pools
- `FundPool.releaseFunds()` checks `factory.isVerified(ngo)` at release-time, not at pool creation
- Any pending multi-sig approvals for that NGO will fail when executed
- The NGO's existing SBTs remain (reputation is permanent, even for revoked NGOs)

---

## Contract 3: `FundPool.sol`

### Purpose
Per-cause escrow contract. Holds donated USDC, tracks proof submissions with IPFS CIDs, and releases funds after multi-sig admin approval. Enforces immutable spending caps.

> [!NOTE]
> Renamed from `CrisisPool` — NGOs now create pools for any cause, not just crises.

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Single NGO per pool (the creator)** | NGOs create their own pools. No `assignNGO()` function. Creator is the only fund recipient. |
| **IPFS CID stored as `string`** | CIDs vary in length. Submissions are infrequent (gas acceptable). Simpler than `bytes32` prefix-stripping. |
| **Proof struct stored on-chain** | Contract needs to read proof data during `releaseFunds()` (check `released` flag, read `amount`). Cannot use events-only. |
| **MultiSig controls release** | `releaseFunds()` callable only by MultiSigAdmin. Replaces single backend verifier. |
| **SBT auto-mints on release** | After successful release, pool calls SBT contract to mint reputation token. |
| **SafeERC20 for all token ops** | Industry standard. Handles non-standard ERC20 return values. |
| **ReentrancyGuard on fund ops** | Belt-and-suspenders with CEI pattern. Applied to `donate()` and `releaseFunds()`. |
| **Pausable for donations only** | Emergency stop for donations. Does NOT affect proof submission or fund release. |
| **Immutable caps** | Set in constructor. No setter functions. Cannot be changed by anyone after deployment. |

### State

**Immutable (set in constructor, never changeable):**

| Variable | Type | Description |
|---|---|---|
| `maxPerClaim` | `uint256` | Max USDC (6 decimals) per single release |
| `maxPerNGOPerDay` | `uint256` | Max USDC the creator can receive per UTC day |
| `maxPerNGOPool` | `uint256` | Max USDC the creator can receive from this pool total |
| `usdc` | `IERC20` | USDC token contract |
| `factory` | `address` | PoolFactory address (for `isVerified` checks) |
| `multiSigAdmin` | `address` | MultiSigAdmin contract address |
| `creator` | `address` | NGO wallet that created this pool |
| `sbt` | `LivanaSBT` | SBT contract address |

**Mutable:**

| Variable | Type | Description |
|---|---|---|
| `donationsPaused` | `bool` | Whether new donations are paused |
| `totalDonated` | `uint256` | Cumulative USDC donated to this pool |
| `totalReleased` | `uint256` | Cumulative USDC released from this pool |
| `totalClaimedByCreator` | `uint256` | Lifetime USDC claimed by the creator NGO |
| `dailyClaimed` | `uint256` | Amount claimed by creator today (UTC) |
| `lastClaimDay` | `uint256` | UTC day number of last claim |
| `proofCount` | `uint256` | Total proof submissions |
| `proofs` | `mapping(uint256 => ProofSubmission)` | proofId → submission data |

**ProofSubmission struct:**
```
{
    string ipfsCid,       // IPFS folder CID containing all proof documents
    uint256 amount,       // Claimed reimbursement amount (USDC, 6 decimals)
    uint256 timestamp,    // Block timestamp of submission
    bool released         // Whether funds have been released for this proof
}
```

### Functions

| Function | Who Can Call | What It Does |
|---|---|---|
| `donate(amount)` | Anyone (when not paused) | Donate USDC to pool. Requires prior `usdc.approve()`. Uses `SafeERC20.safeTransferFrom()`. |
| `submitProof(ipfsCid, amount)` | Creator NGO only | Submit IPFS CID + claimed amount. Returns `proofId`. |
| `releaseFunds(proofId)` | MultiSigAdmin only | Release USDC for a specific proof. Enforces all caps. Mints SBT. |
| `pauseDonations()` | MultiSigAdmin only | Pause new donations. |
| `resumeDonations()` | MultiSigAdmin only | Resume donations. |
| `getPoolBalance()` | Anyone (view) | Current USDC balance held by this pool. |
| `getDailyClaimed()` | Anyone (view) | Amount claimed today (resets at UTC midnight). |
| `getProof(proofId)` | Anyone (view) | Returns proof details. |

### The `releaseFunds()` Flow

This is the most critical function. Here's exactly what happens:

```
MultiSigAdmin calls pool.releaseFunds(proofId) after 2-of-3 approval

Step 1 — CHECKS:
  ├── Is creator NGO still verified in PoolFactory? (revocation check)
  ├── Does proofId exist?
  ├── Has this proof already been released? (double-release prevention)
  ├── Is amount ≤ maxPerClaim?
  ├── Is dailyClaimed + amount ≤ maxPerNGOPerDay? (reset if new UTC day)
  ├── Is totalClaimedByCreator + amount ≤ maxPerNGOPool?
  └── Does pool have enough USDC balance?

Step 2 — EFFECTS:
  ├── Mark proof as released
  ├── Update daily counter (reset if new day)
  ├── Update total claimed counter
  └── Update total released counter

Step 3 — INTERACTIONS:
  ├── SafeERC20.safeTransfer(creator, amount)  ← USDC moves here
  └── sbt.mint(creator, amount)                ← SBT minted here
```

> [!CAUTION]
> Funds can **only** leave a FundPool via `releaseFunds()` to the verified creator NGO. There is no `withdraw()`, no `closePool()`, no admin override for fund movement. The contract has no `selfdestruct`, no `delegatecall`, no `receive()` / `fallback()`.

### Events
- `DonationReceived(donor, amount)`
- `ProofSubmitted(proofId, ipfsCid, amount)`
- `FundsReleased(proofId, ngo, amount)`
- `DonationsPaused()`
- `DonationsResumed()`

---

## Contract 4: `LivanaSBT.sol`

### Purpose
Soulbound Reputation Token. ERC-721 NFT that **cannot be transferred**. Minted after every successful fund release. Forms a permanent, on-chain, publicly verifiable NGO reputation history.

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **EIP-5192 compliant** | Standard interface for soulbound detection. Wallets/marketplaces can programmatically identify non-transferable tokens. |
| **OZ v5 `_update` override** | `_beforeTokenTransfer` was removed in OZ v5. `_update` is the single hook for mint/transfer/burn. Block transfers where `from != address(0)`. |
| **Only FundPool contracts can mint** | Checked via `PoolFactory.isPool(msg.sender)`. Prevents arbitrary SBT creation. |
| **Burns disabled** | SBTs are permanent. Reputation cannot be erased — by design. |
| **No tokenURI** | MVP doesn't need off-chain metadata/images. On-chain struct is sufficient. Can add later. |
| **Reputation data stored on-chain** | Each SBT stores pool address, NGO address, verified amount, and timestamp. Publicly readable. |

### State

| Variable | Type | Description |
|---|---|---|
| `factory` | `IPoolFactory immutable` | PoolFactory address (for `isPool` mint access control) |
| `tokenCount` | `uint256` | Total SBTs minted (also tokenId counter) |
| `reputation` | `mapping(uint256 => ReputationData)` | tokenId → reputation data |

**ReputationData struct:**
```
{
    address pool,       // FundPool that triggered this mint
    address ngo,        // NGO wallet that received the SBT
    uint256 amount,     // USDC amount verified and released
    uint256 timestamp   // Block timestamp of minting
}
```

### Functions

| Function | Who Can Call | What It Does |
|---|---|---|
| `mint(ngo, amount)` | FundPool contracts only | Mint SBT to NGO. Stores reputation data. Emits `Locked`. |
| `locked(tokenId)` | Anyone (view) | Always returns `true`. EIP-5192 compliance. |
| `supportsInterface(interfaceId)` | Anyone (view) | Returns `true` for `0xb45a3c0e` (EIP-5192) + ERC-721 interfaces. |
| `getReputation(tokenId)` | Anyone (view) | Returns full ReputationData struct. |
| `balanceOf(ngo)` | Anyone (view) | Inherited from ERC-721. Returns SBT count = number of successful projects. |

### Transfer Blocking Logic

```
_update(to, tokenId, auth):
    from = _ownerOf(tokenId)
    
    if from != address(0):    ← not a mint
        REVERT "soulbound, transfer blocked"
    
    return super._update(to, tokenId, auth)
```

This blocks:
- ✅ Transfers between wallets → **BLOCKED**
- ✅ Burns (to = address(0), from ≠ address(0)) → **BLOCKED**
- ✅ Mints (from = address(0)) → **ALLOWED**

---

## Inter-Contract Call Map

```
MultiSigAdmin ──call──→ PoolFactory.addVerifiedNGO(ngo)
MultiSigAdmin ──call──→ PoolFactory.revokeNGO(ngo)
MultiSigAdmin ──call──→ FundPool.releaseFunds(proofId)
MultiSigAdmin ──call──→ FundPool.pauseDonations()
MultiSigAdmin ──call──→ FundPool.resumeDonations()

Verified NGO  ──call──→ PoolFactory.deployPool(caps...)     → creates FundPool
Creator NGO   ──call──→ FundPool.submitProof(cid, amount)
Anyone        ──call──→ FundPool.donate(amount)

FundPool      ──call──→ PoolFactory.isVerified(ngo)          (at release-time)
FundPool      ──call──→ LivanaSBT.mint(ngo, amount)        (after release)
LivanaSBT   ──call──→ PoolFactory.isPool(msg.sender)       (mint access control)
```

---

## Deployment Strategy

### Order
```
1. Deploy MockUSDC (testnet) or use existing USDC (mainnet)
2. Deploy PoolFactory(multiSigAddress, usdcAddress)
       → PoolFactory deploys LivanaSBT internally in its constructor
3. Deploy MultiSigAdmin([signer1, signer2, signer3])
4. Call PoolFactory.setMultiSigAdmin(multiSigAdminAddress)  ← one-time setup
```

> [!NOTE]
> **Circular dependency resolution:** PoolFactory needs the MultiSigAdmin address, but MultiSigAdmin is deployed after. Solution: PoolFactory has a one-time `setMultiSigAdmin()` function with an `initialized` guard. Once set, it cannot be changed.

### Chain Target
- **Production:** Polygon PoS (low gas, strong USDC liquidity)
- **Testing:** Local Anvil chain with MockUSDC
- **Gas costs:** All operations are fractions of a cent on Polygon (~30-50 gwei gas price)

---

## Security Model

### Immutability Guarantees
- All contracts are **non-upgradable** (no proxy pattern)
- Spending caps are **immutable** (set in constructor, no setters)
- SBTs are **non-transferable** and **non-burnable**
- No `selfdestruct` in any contract
- No `delegatecall` in any contract
- No `receive()` or `fallback()` in fund-handling contracts

### Access Control Summary

| Action | Required Access |
|---|---|
| Verify/revoke NGO | 2-of-3 multi-sig |
| Release funds | 2-of-3 multi-sig |
| Pause/resume donations | 2-of-3 multi-sig |
| Create a pool | Verified NGO |
| Submit proof | Pool creator NGO |
| Donate | Anyone |
| Mint SBT | FundPool contracts only |

### Attack Vectors Mitigated

| Attack | Mitigation |
|---|---|
| Forged receipt drains pool | Multi-sig requires 2 humans to review IPFS docs before release |
| Single compromised admin | 2-of-3 threshold — one signer can't do anything alone |
| NGO drains pool quickly | Immutable per-claim, daily, and lifetime caps enforced by contract |
| Double-release on same proof | `released` flag checked in `releaseFunds()` |
| Reentrancy on donate/release | `ReentrancyGuard` + CEI pattern |
| Non-standard ERC20 issues | `SafeERC20` wraps all token interactions |
| Fake SBT minting | `isPool(msg.sender)` check — only factory-deployed pools can mint |
| Revoked NGO still gets paid | `isVerified()` checked at release-time, not at pool creation |

---

## OpenZeppelin Dependencies

| Contract | OZ Imports |
|---|---|
| MultiSigAdmin | None (standalone) |
| PoolFactory | `IERC20`, `SafeERC20` |
| FundPool | `IERC20`, `SafeERC20`, `ReentrancyGuard`, `Pausable` |
| LivanaSBT | `ERC721` |

All using **OpenZeppelin v5.6.1** (already installed in `lib/openzeppelin-contracts`).

---

## File Structure (Post-Implementation)

```
smartcontracts/src/
├── MultiSigAdmin.sol          # 2-of-3 multi-sig governance
├── PoolFactory.sol            # NGO registry + pool deployer + SBT deployer
├── FundPool.sol               # Per-cause escrow with IPFS proof tracking
├── LivanaSBT.sol            # EIP-5192 soulbound reputation token
├── interfaces/
│   └── IPoolFactory.sol       # Interface: isVerified() + isPool()
└── mocks/
    └── MockUSDC.sol           # 6-decimal ERC20 for testing
```
