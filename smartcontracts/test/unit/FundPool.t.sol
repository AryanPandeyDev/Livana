// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {BaseTest} from "../helpers/BaseTest.sol";
import {FundPool} from "../../src/FundPool.sol";

/**
 * @title FundPoolTest
 * @notice Unit tests for FundPool: donations, proof submission, fund release,
 *         pause controls, and all edge cases.
 */
contract FundPoolTest is BaseTest {
    FundPool pool;

    function setUp() public override {
        super.setUp();
        pool = _verifyAndDeployPool(ngo1);
    }

    // =====================================================================
    //  Constructor
    // =====================================================================

    function test_constructor_setsImmutables() public view {
        assertEq(address(pool.usdc()), address(usdc));
        assertEq(address(pool.factory()), address(factory));
        assertEq(pool.multiSigAdmin(), multiSig);
        assertEq(pool.creator(), ngo1);
        assertEq(address(pool.sbt()), address(sbt));
        assertEq(pool.metadataCid(), DEFAULT_METADATA_CID);
    }

    function test_constructor_revertsOnZeroAddress() public {
        vm.expectRevert(FundPool.ZeroAddress.selector);
        new FundPool(address(0), address(factory), multiSig, ngo1, address(sbt), DEFAULT_METADATA_CID);
    }

    // =====================================================================
    //  Donate
    // =====================================================================

    function test_donate_works() public {
        _donate(donor1, pool, USDC_100);
        assertEq(pool.totalDonated(), USDC_100);
        assertEq(pool.getPoolBalance(), USDC_100);
    }

    function test_donate_emitsEvent() public {
        vm.startPrank(donor1);
        usdc.approve(address(pool), USDC_100);
        vm.expectEmit(true, false, false, true);
        emit FundPool.DonationReceived(donor1, USDC_100);
        pool.donate(USDC_100);
        vm.stopPrank();
    }

    function test_donate_multipleDonors() public {
        _donate(donor1, pool, USDC_100);
        _donate(donor2, pool, USDC_1000);
        assertEq(pool.totalDonated(), USDC_100 + USDC_1000);
        assertEq(pool.getPoolBalance(), USDC_100 + USDC_1000);
    }

    function test_donate_revertsOnZeroAmount() public {
        vm.prank(donor1);
        vm.expectRevert(FundPool.ZeroAmount.selector);
        pool.donate(0);
    }

    function test_donate_revertsWhenPaused() public {
        vm.prank(multiSig);
        pool.pauseDonations();

        vm.startPrank(donor1);
        usdc.approve(address(pool), USDC_100);
        vm.expectRevert(); // EnforcedPause
        pool.donate(USDC_100);
        vm.stopPrank();
    }

    function test_donate_revertsWithoutApproval() public {
        vm.prank(donor1);
        vm.expectRevert(); // SafeERC20 revert
        pool.donate(USDC_100);
    }

    // =====================================================================
    //  Proof Submission
    // =====================================================================

    function test_submitProof_works() public {
        vm.prank(ngo1);
        uint256 id = pool.submitProof("QmTestCID123", USDC_100);
        assertEq(id, 0);
        assertEq(pool.proofCount(), 1);

        (string memory cid, uint256 amount, uint256 ts, bool released) = pool.getProof(0);
        assertEq(cid, "QmTestCID123");
        assertEq(amount, USDC_100);
        assertTrue(ts > 0);
        assertFalse(released);
    }

    function test_submitProof_emitsEvent() public {
        vm.prank(ngo1);
        vm.expectEmit(true, false, false, true);
        emit FundPool.ProofSubmitted(0, "QmTestCID123", USDC_100);
        pool.submitProof("QmTestCID123", USDC_100);
    }

    function test_submitProof_incrementsId() public {
        _submitProof(pool, ngo1, "QmCID1", USDC_100);
        _submitProof(pool, ngo1, "QmCID2", USDC_100);
        _submitProof(pool, ngo1, "QmCID3", USDC_100);
        assertEq(pool.proofCount(), 3);
    }

    function test_submitProof_revertsNotCreator() public {
        vm.prank(stranger);
        vm.expectRevert(FundPool.NotCreator.selector);
        pool.submitProof("QmCID", USDC_100);
    }

    function test_submitProof_revertsEmptyCID() public {
        vm.prank(ngo1);
        vm.expectRevert(FundPool.EmptyCID.selector);
        pool.submitProof("", USDC_100);
    }

    function test_submitProof_revertsZeroAmount() public {
        vm.prank(ngo1);
        vm.expectRevert(FundPool.ZeroAmount.selector);
        pool.submitProof("QmCID", 0);
    }

    // =====================================================================
    //  Release Funds
    // =====================================================================

    function test_releaseFunds_works() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);

        uint256 ngoBefore = usdc.balanceOf(ngo1);
        _releaseFunds(pool, 0);

        assertEq(usdc.balanceOf(ngo1), ngoBefore + USDC_100);
        assertEq(pool.totalReleased(), USDC_100);
        assertEq(pool.getPoolBalance(), USDC_1000 - USDC_100);
    }

    function test_releaseFunds_emitsEvent() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);

        vm.prank(multiSig);
        vm.expectEmit(true, true, false, true);
        emit FundPool.FundsReleased(0, ngo1, USDC_100);
        pool.releaseFunds(0);
    }

    function test_releaseFunds_mintsSBT() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);
        _releaseFunds(pool, 0);

        assertEq(sbt.tokenCount(), 1);
        assertEq(sbt.ownerOf(0), ngo1);

        (address sbtPool, uint256 amount, uint256 ts) = sbt.getReputation(0);
        assertEq(sbtPool, address(pool));
        assertEq(amount, USDC_100);
        assertTrue(ts > 0);
    }

    function test_releaseFunds_revertsNotMultiSig() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);

        vm.prank(stranger);
        vm.expectRevert(FundPool.NotMultiSig.selector);
        pool.releaseFunds(0);
    }

    function test_releaseFunds_revertsProofDoesNotExist() public {
        vm.prank(multiSig);
        vm.expectRevert(FundPool.ProofDoesNotExist.selector);
        pool.releaseFunds(0);
    }

    function test_releaseFunds_revertsProofAlreadyReleased() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);
        _releaseFunds(pool, 0);

        vm.prank(multiSig);
        vm.expectRevert(FundPool.ProofAlreadyReleased.selector);
        pool.releaseFunds(0);
    }

    function test_releaseFunds_revertsInsufficientBalance() public {
        _donate(donor1, pool, USDC_100);
        _submitProof(pool, ngo1, "QmProof1", USDC_1000); // claim > balance

        vm.prank(multiSig);
        vm.expectRevert(
            abi.encodeWithSelector(
                FundPool.InsufficientPoolBalance.selector,
                USDC_1000,
                USDC_100
            )
        );
        pool.releaseFunds(0);
    }

    function test_releaseFunds_revertsIfNGORevoked() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);

        // Revoke the NGO
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);

        vm.prank(multiSig);
        vm.expectRevert(FundPool.NGONotVerified.selector);
        pool.releaseFunds(0);
    }

    function test_releaseFunds_multipleProofs() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);
        _submitProof(pool, ngo1, "QmProof2", USDC_100);
        _submitProof(pool, ngo1, "QmProof3", USDC_100);

        _releaseFunds(pool, 0);
        _releaseFunds(pool, 2); // skip proof 1
        _releaseFunds(pool, 1);

        assertEq(pool.totalReleased(), USDC_100 * 3);
        assertEq(sbt.tokenCount(), 3);
    }

    // =====================================================================
    //  Pause / Resume
    // =====================================================================

    function test_pauseDonations_blocksNewDonations() public {
        vm.prank(multiSig);
        pool.pauseDonations();

        vm.startPrank(donor1);
        usdc.approve(address(pool), USDC_100);
        vm.expectRevert(); // EnforcedPause
        pool.donate(USDC_100);
        vm.stopPrank();
    }

    function test_pauseDonations_doesNotBlockProofSubmission() public {
        vm.prank(multiSig);
        pool.pauseDonations();

        // Proof submission still works while paused
        vm.prank(ngo1);
        pool.submitProof("QmCID", USDC_100);
        assertEq(pool.proofCount(), 1);
    }

    function test_pauseDonations_doesNotBlockRelease() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmProof1", USDC_100);

        vm.prank(multiSig);
        pool.pauseDonations();

        // Release still works while paused
        _releaseFunds(pool, 0);
        assertEq(pool.totalReleased(), USDC_100);
    }

    function test_resumeDonations_works() public {
        vm.prank(multiSig);
        pool.pauseDonations();

        vm.prank(multiSig);
        pool.resumeDonations();

        // Donations work again
        _donate(donor1, pool, USDC_100);
        assertEq(pool.totalDonated(), USDC_100);
    }

    function test_pause_revertsNotMultiSig() public {
        vm.prank(stranger);
        vm.expectRevert(FundPool.NotMultiSig.selector);
        pool.pauseDonations();
    }

    function test_resume_revertsNotMultiSig() public {
        vm.prank(multiSig);
        pool.pauseDonations();

        vm.prank(stranger);
        vm.expectRevert(FundPool.NotMultiSig.selector);
        pool.resumeDonations();
    }

    // =====================================================================
    //  View Functions
    // =====================================================================

    function test_getPoolBalance_reflectsDonationsAndReleases() public {
        _donate(donor1, pool, USDC_1000);
        assertEq(pool.getPoolBalance(), USDC_1000);

        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);
        assertEq(pool.getPoolBalance(), USDC_1000 - USDC_100);
    }

    function test_getProof_revertsOnInvalidId() public {
        vm.expectRevert(FundPool.ProofDoesNotExist.selector);
        pool.getProof(999);
    }

    // =====================================================================
    //  ETH Rejection
    // =====================================================================

    function test_rejectsETH() public {
        vm.deal(donor1, 1 ether);
        vm.prank(donor1);
        (bool success,) = address(pool).call{value: 1 ether}("");
        assertFalse(success);
    }
}
