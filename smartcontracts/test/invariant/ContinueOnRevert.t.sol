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
//  Handler: Open (continue_on_revert)
//  Calls contract functions with RAW fuzzed inputs — no bounding, no
//  guards. Many calls will revert (wrong caller, zero amount, etc.) and
//  that's expected. The point is to ensure invariants hold even when the
//  fuzzer throws garbage at the system.
// =========================================================================

contract ContinueOnRevertHandler is Test {
    FundPool public pool;
    MockUSDC public usdc;
    PoolFactory public factory;
    address public multiSig;
    address public ngo;
    address public donor;

    // Callers the fuzzer can impersonate
    address[] public actors;

    constructor(
        FundPool _pool,
        MockUSDC _usdc,
        PoolFactory _factory,
        address _multiSig,
        address _ngo,
        address _donor
    ) {
        pool = _pool;
        usdc = _usdc;
        factory = _factory;
        multiSig = _multiSig;
        ngo = _ngo;
        donor = _donor;

        actors.push(_multiSig);
        actors.push(_ngo);
        actors.push(_donor);
        actors.push(makeAddr("attacker"));
    }

    /// @dev Pick a random caller from the actor set.
    function _randomActor(uint256 seed) internal view returns (address) {
        return actors[seed % actors.length];
    }

    /// @dev Attempt a donation from a random caller with a random amount.
    ///      Will revert if: caller has no USDC, amount is 0, pool is paused, etc.
    function donate(uint256 amount, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);

        // Give the caller some USDC so SOME calls succeed
        usdc.mint(caller, amount);

        vm.startPrank(caller);
        usdc.approve(address(pool), amount);
        pool.donate(amount);
        vm.stopPrank();
    }

    /// @dev Attempt proof submission from a random caller.
    ///      Will revert if: not creator, empty CID, zero amount.
    function submitProof(uint256 amount, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        pool.submitProof("QmOpenCID", amount);
    }

    /// @dev Attempt to submit proof with empty CID.
    function submitProofEmptyCID(uint256 amount) external {
        vm.prank(ngo);
        pool.submitProof("", amount);
    }

    /// @dev Attempt fund release from a random caller with a random proof index.
    ///      Will revert if: not multiSig, invalid proof ID, already released, etc.
    function releaseFunds(uint256 proofIndex, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        pool.releaseFunds(proofIndex);
    }

    /// @dev Attempt to pause from random caller.
    function pauseDonations(uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        pool.pauseDonations();
    }

    /// @dev Attempt to resume from random caller.
    function resumeDonations(uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        pool.resumeDonations();
    }

    /// @dev Attempt to verify an NGO from random caller.
    function addVerifiedNGO(address ngoAddr, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        factory.addVerifiedNGO(ngoAddr);
    }

    /// @dev Attempt to revoke an NGO from random caller.
    function revokeNGO(address ngoAddr, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        factory.revokeNGO(ngoAddr);
    }

    /// @dev Attempt to transfer an SBT (should always revert).
    function transferSBT(uint256 tokenId, address to, uint256 actorSeed) external {
        address caller = _randomActor(actorSeed);
        vm.prank(caller);
        LivanaSBT(address(pool.sbt())).transferFrom(caller, to, tokenId);
    }
}

// =========================================================================
//  Invariant Test: continue_on_revert (default)
//  The fuzzer throws garbage at the system — wrong callers, invalid
//  inputs, zero amounts, out-of-range proof IDs. Most calls revert.
//  We verify that invariants STILL hold despite the chaos.
// =========================================================================

/// @custom:invariant-config fail_on_revert = false
contract ContinueOnRevertInvariantTest is StdInvariant, Test {
    ContinueOnRevertHandler handler;
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
        address poolAddr = factory.deployPool("QmContinueOnRevertCID");
        pool = FundPool(poolAddr);

        handler = new ContinueOnRevertHandler(
            pool, usdc, factory, multiSig, ngo, donor
        );
        targetContract(address(handler));
    }

    // --- These invariants must hold even under adversarial/chaotic inputs ---

    /// @dev Funds out can never exceed funds in.
    function invariant_releasedNeverExceedsDonated() public view {
        assertLe(pool.totalReleased(), pool.totalDonated());
    }

    /// @dev USDC balance must always equal donated minus released.
    function invariant_balanceEquation() public view {
        assertEq(
            pool.getPoolBalance(),
            pool.totalDonated() - pool.totalReleased()
        );
    }

    /// @dev Pool must always be registered in the factory.
    function invariant_poolIsRegistered() public view {
        assertTrue(factory.isPool(address(pool)));
    }

    /// @dev Every minted SBT must be owned by the NGO (can't be transferred).
    function invariant_sbtOwnership() public view {
        uint256 count = sbt.tokenCount();
        for (uint256 i = 0; i < count; i++) {
            assertEq(sbt.ownerOf(i), ngo);
        }
    }

    /// @dev Every minted SBT must report as locked (EIP-5192).
    function invariant_sbtAlwaysLocked() public view {
        uint256 count = sbt.tokenCount();
        for (uint256 i = 0; i < count; i++) {
            assertTrue(sbt.locked(i));
        }
    }

    /// @dev The NGO's verification status can only be changed by the multiSig,
    ///      so random calls shouldn't flip it. If it did get revoked (by multiSig
    ///      via the handler), that's fine — but the pool's creator should still
    ///      match the original NGO.
    function invariant_creatorNeverChanges() public view {
        assertEq(pool.creator(), ngo);
    }

    /// @dev multiSigAdmin on the factory should never change after being set.
    function invariant_multiSigNeverChanges() public view {
        assertEq(factory.multiSigAdmin(), multiSig);
    }
}
