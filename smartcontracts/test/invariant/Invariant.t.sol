// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Test} from "forge-std/Test.sol";
import {StdInvariant} from "forge-std/StdInvariant.sol";
import {FundPool} from "../../src/FundPool.sol";
import {PoolFactory} from "../../src/PoolFactory.sol";
import {LivanaSBT} from "../../src/LivanaSBT.sol";
import {MockUSDC} from "../../src/mocks/MockUSDC.sol";
import {DeployLivana} from "../../script/DeployLivana.s.sol";

// =========================================================================
//  Handler: Tight (fail_on_revert = true)
//  Every call MUST succeed. Inputs are bounded and state is guarded so
//  that no function ever reverts. If one does, the invariant test fails —
//  meaning our handler logic has a gap.
// =========================================================================

contract FailOnRevertHandler is Test {
    FundPool public pool;
    MockUSDC public usdc;
    address public multiSig;
    address public ngo;
    address public donor;

    // Ghost variables
    uint256 public ghost_totalDonated;
    uint256 public ghost_totalReleased;
    uint256 public ghost_proofsSubmitted;
    uint256 public ghost_proofsReleased;

    constructor(
        FundPool _pool,
        MockUSDC _usdc,
        address _multiSig,
        address _ngo,
        address _donor
    ) {
        pool = _pool;
        usdc = _usdc;
        multiSig = _multiSig;
        ngo = _ngo;
        donor = _donor;
    }

    /// @dev Always succeeds — amount is bounded to a valid range.
    function donate(uint256 amount) external {
        amount = bound(amount, 1, 1000e6);

        usdc.mint(donor, amount);

        vm.startPrank(donor);
        usdc.approve(address(pool), amount);
        pool.donate(amount);
        vm.stopPrank();

        ghost_totalDonated += amount;
    }

    /// @dev Always succeeds — non-empty CID, non-zero amount.
    function submitProof(uint256 amount) external {
        amount = bound(amount, 1, 500e6);

        vm.prank(ngo);
        pool.submitProof("QmInvariantCID", amount);

        ghost_proofsSubmitted++;
    }

    /// @dev Guarded — only calls releaseFunds when preconditions are met.
    ///      Skips (returns early) if no valid proof exists.
    function releaseFunds(uint256 proofIndex) external {
        uint256 count = pool.proofCount();
        if (count == 0) return;

        proofIndex = bound(proofIndex, 0, count - 1);

        (, uint256 amount,, bool released) = pool.getProof(proofIndex);
        if (released) return;

        uint256 balance = pool.getPoolBalance();
        if (amount > balance) return;

        vm.prank(multiSig);
        pool.releaseFunds(proofIndex);

        ghost_totalReleased += amount;
        ghost_proofsReleased++;
    }
}

// =========================================================================
//  Invariant Test: fail_on_revert
//  Uses the tight handler. Zero reverts expected. Proves that the handler
//  correctly models all valid state transitions AND that invariants hold
//  when every single operation succeeds.
// =========================================================================

/// @custom:invariant-config fail_on_revert = true
contract FailOnRevertInvariantTest is StdInvariant, Test {
    FailOnRevertHandler handler;
    FundPool pool;
    MockUSDC usdc;
    PoolFactory factory;
    LivanaSBT sbt;

    address multiSig = makeAddr("multiSig");
    address ngo = makeAddr("ngo");
    address donor = makeAddr("donor");

    function setUp() public {
        DeployLivana deployer = new DeployLivana();
        DeployLivana.Deployment memory d = deployer.deployWithMockUSDC(multiSig);

        usdc = d.usdc;
        factory = d.factory;
        sbt = d.sbt;

        vm.prank(multiSig);
        factory.addVerifiedNGO(ngo);
        vm.prank(ngo);
        address poolAddr = factory.deployPool("QmInvariantTestCID");
        pool = FundPool(poolAddr);

        handler = new FailOnRevertHandler(pool, usdc, multiSig, ngo, donor);
        targetContract(address(handler));
    }

    // --- Core accounting invariants ---

    function invariant_releasedNeverExceedsDonated() public view {
        assertLe(pool.totalReleased(), pool.totalDonated());
    }

    function invariant_balanceEquation() public view {
        assertEq(
            pool.getPoolBalance(),
            pool.totalDonated() - pool.totalReleased()
        );
    }

    // --- Ghost ↔ contract consistency ---

    function invariant_ghostMatchesDonated() public view {
        assertEq(handler.ghost_totalDonated(), pool.totalDonated());
    }

    function invariant_ghostMatchesReleased() public view {
        assertEq(handler.ghost_totalReleased(), pool.totalReleased());
    }

    // --- SBT invariants ---

    function invariant_sbtCountMatchesReleases() public view {
        assertEq(sbt.tokenCount(), handler.ghost_proofsReleased());
    }

    function invariant_sbtOwnership() public view {
        uint256 count = sbt.tokenCount();
        for (uint256 i = 0; i < count; i++) {
            assertEq(sbt.ownerOf(i), ngo);
        }
    }

    // --- Structural invariants ---

    function invariant_proofCount() public view {
        assertEq(pool.proofCount(), handler.ghost_proofsSubmitted());
    }

    function invariant_poolIsRegistered() public view {
        assertTrue(factory.isPool(address(pool)));
    }
}
