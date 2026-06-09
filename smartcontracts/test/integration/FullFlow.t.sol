// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {BaseTest} from "../helpers/BaseTest.sol";
import {FundPool} from "../../src/FundPool.sol";

/**
 * @title FullFlowTest
 * @notice Integration tests covering complete end-to-end flows:
 *         onboarding → pool creation → donation → proof → release → SBT.
 */
contract FullFlowTest is BaseTest {
    // =====================================================================
    //  Happy Path: Complete lifecycle
    // =====================================================================

    function test_fullLifecycle() public {
        // 1. Verify NGO
        _verifyNGO(ngo1);
        assertTrue(factory.isVerified(ngo1));

        // 2. NGO deploys a pool
        FundPool pool = _deployPool(ngo1);
        assertTrue(factory.isPool(address(pool)));

        // 3. Donors contribute
        _donate(donor1, pool, USDC_1000);
        _donate(donor2, pool, USDC_1000);
        assertEq(pool.totalDonated(), USDC_1000 * 2);

        // 4. NGO submits proofs
        _submitProof(pool, ngo1, "QmReceipts1", 500e6);
        _submitProof(pool, ngo1, "QmReceipts2", 300e6);

        // 5. Multi-sig releases proof 0
        _releaseFunds(pool, 0);
        assertEq(usdc.balanceOf(ngo1), 500e6);
        assertEq(sbt.tokenCount(), 1);

        // 6. Multi-sig releases proof 1
        _releaseFunds(pool, 1);
        assertEq(usdc.balanceOf(ngo1), 800e6);
        assertEq(sbt.tokenCount(), 2);

        // 7. Verify SBTs
        assertEq(sbt.ownerOf(0), ngo1);
        assertEq(sbt.ownerOf(1), ngo1);
        assertEq(pool.totalReleased(), 800e6);
        assertEq(pool.getPoolBalance(), USDC_1000 * 2 - 800e6);
    }

    // =====================================================================
    //  Multiple NGOs, multiple pools
    // =====================================================================

    function test_multipleNGOsMultiplePools() public {
        // Two NGOs, each with their own pool
        FundPool pool1 = _verifyAndDeployPool(ngo1);
        FundPool pool2 = _verifyAndDeployPool(ngo2);

        // Same donor funds both pools
        _donate(donor1, pool1, USDC_1000);
        _donate(donor1, pool2, USDC_1000);

        // Each NGO submits proof
        _submitProof(pool1, ngo1, "QmNGO1Proof", USDC_100);
        _submitProof(pool2, ngo2, "QmNGO2Proof", USDC_100);

        // Multi-sig releases both
        _releaseFunds(pool1, 0);
        _releaseFunds(pool2, 0);

        // Each NGO got their funds
        assertEq(usdc.balanceOf(ngo1), USDC_100);
        assertEq(usdc.balanceOf(ngo2), USDC_100);

        // Each got an SBT
        assertEq(sbt.tokenCount(), 2);
        assertEq(sbt.ownerOf(0), ngo1);
        assertEq(sbt.ownerOf(1), ngo2);
    }

    // =====================================================================
    //  Revocation mid-flow: NGO verified → pool created → NGO revoked → release blocked
    // =====================================================================

    function test_revocationBlocksRelease() public {
        FundPool pool = _verifyAndDeployPool(ngo1);
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof", USDC_100);

        // Revoke NGO before release
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);

        // Release should now fail
        vm.prank(multiSig);
        vm.expectRevert(FundPool.NGONotVerified.selector);
        pool.releaseFunds(0);
    }

    function test_reverifyAllowsRelease() public {
        FundPool pool = _verifyAndDeployPool(ngo1);
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof", USDC_100);

        // Revoke then re-verify
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);
        _verifyNGO(ngo1);

        // Release works again
        _releaseFunds(pool, 0);
        assertEq(usdc.balanceOf(ngo1), USDC_100);
    }

    // =====================================================================
    //  Pool isolation: one pool's state doesn't affect another
    // =====================================================================

    function test_poolIsolation() public {
        _verifyNGO(ngo1);
        FundPool poolA = _deployPool(ngo1);
        FundPool poolB = _deployPool(ngo1);

        _donate(donor1, poolA, USDC_1000);
        _donate(donor1, poolB, USDC_100);

        _submitProof(poolA, ngo1, "QmProofA", USDC_1000);
        _submitProof(poolB, ngo1, "QmProofB", USDC_100);

        // Release from pool B first
        _releaseFunds(poolB, 0);
        assertEq(poolB.totalReleased(), USDC_100);
        assertEq(poolA.totalReleased(), 0); // Pool A unaffected

        // Release from pool A
        _releaseFunds(poolA, 0);
        assertEq(poolA.totalReleased(), USDC_1000);
    }

    // =====================================================================
    //  Pause during active flow
    // =====================================================================

    function test_pauseMidFlow() public {
        FundPool pool = _verifyAndDeployPool(ngo1);
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof", USDC_100);

        // Pause donations
        vm.prank(multiSig);
        pool.pauseDonations();

        // Can't donate
        vm.startPrank(donor2);
        usdc.approve(address(pool), USDC_100);
        vm.expectRevert(); // EnforcedPause
        pool.donate(USDC_100);
        vm.stopPrank();

        // But can still submit proofs and release
        _submitProof(pool, ngo1, "QmProof2", USDC_100);
        _releaseFunds(pool, 0);
        assertEq(pool.totalReleased(), USDC_100);

        // Resume
        vm.prank(multiSig);
        pool.resumeDonations();
        _donate(donor2, pool, USDC_100);
        assertEq(pool.totalDonated(), USDC_1000 + USDC_100);
    }

    // =====================================================================
    //  Drain entire pool balance across multiple proofs
    // =====================================================================

    function test_drainFullBalance() public {
        FundPool pool = _verifyAndDeployPool(ngo1);
        _donate(donor1, pool, USDC_1000);

        // Submit proofs totaling exactly the donated amount
        _submitProof(pool, ngo1, "QmBatch1", 400e6);
        _submitProof(pool, ngo1, "QmBatch2", 400e6);
        _submitProof(pool, ngo1, "QmBatch3", 200e6);

        _releaseFunds(pool, 0);
        _releaseFunds(pool, 1);
        _releaseFunds(pool, 2);

        assertEq(pool.getPoolBalance(), 0);
        assertEq(pool.totalReleased(), USDC_1000);
        assertEq(usdc.balanceOf(ngo1), USDC_1000);
        assertEq(sbt.tokenCount(), 3);
    }
}
