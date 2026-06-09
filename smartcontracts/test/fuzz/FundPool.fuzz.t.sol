// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {BaseTest} from "../helpers/BaseTest.sol";
import {FundPool} from "../../src/FundPool.sol";

/**
 * @title FuzzTest
 * @notice Fuzz tests for FundPool: random donation amounts, proof amounts,
 *         and multiple sequential releases with bounded inputs.
 */
contract FuzzTest is BaseTest {
    FundPool pool;

    function setUp() public override {
        super.setUp();
        pool = _verifyAndDeployPool(ngo1);
    }

    // =====================================================================
    //  Fuzz: Donations
    // =====================================================================

    /// @dev Any non-zero amount that the donor has should donate successfully.
    function testFuzz_donate(uint256 amount) public {
        amount = bound(amount, 1, USDC_10K); // donor1 has 10K USDC

        vm.startPrank(donor1);
        usdc.approve(address(pool), amount);
        pool.donate(amount);
        vm.stopPrank();

        assertEq(pool.totalDonated(), amount);
        assertEq(pool.getPoolBalance(), amount);
    }

    /// @dev Zero donations should always revert.
    function testFuzz_donate_revertsZero(uint8) public {
        vm.prank(donor1);
        vm.expectRevert(FundPool.ZeroAmount.selector);
        pool.donate(0);
    }

    // =====================================================================
    //  Fuzz: Proof submission
    // =====================================================================

    /// @dev Any non-zero amount with non-empty CID should succeed.
    function testFuzz_submitProof(uint256 amount, string calldata cid) public {
        vm.assume(amount > 0);
        vm.assume(bytes(cid).length > 0);

        vm.prank(ngo1);
        uint256 id = pool.submitProof(cid, amount);

        assertEq(id, 0);
        assertEq(pool.proofCount(), 1);
    }

    // =====================================================================
    //  Fuzz: Donate → Proof → Release round trip
    // =====================================================================

    /// @dev Fuzz the full flow: donate a random amount, claim some of it, release.
    function testFuzz_fullRoundTrip(uint256 donateAmount, uint256 claimAmount) public {
        donateAmount = bound(donateAmount, 1, USDC_10K);
        claimAmount = bound(claimAmount, 1, donateAmount);

        // Donate
        vm.startPrank(donor1);
        usdc.approve(address(pool), donateAmount);
        pool.donate(donateAmount);
        vm.stopPrank();

        // Submit proof
        vm.prank(ngo1);
        pool.submitProof("QmFuzzCID", claimAmount);

        // Release
        uint256 ngoBefore = usdc.balanceOf(ngo1);
        vm.prank(multiSig);
        pool.releaseFunds(0);

        // Assertions
        assertEq(usdc.balanceOf(ngo1), ngoBefore + claimAmount);
        assertEq(pool.totalReleased(), claimAmount);
        assertEq(pool.getPoolBalance(), donateAmount - claimAmount);
        assertEq(sbt.tokenCount(), 1);
    }

    // =====================================================================
    //  Fuzz: Multiple sequential releases
    // =====================================================================

    /// @dev Submit N proofs of random amounts that fit within the pool balance.
    function testFuzz_multipleReleases(uint8 numProofs) public {
        numProofs = uint8(bound(numProofs, 1, 10));

        // Donate a large amount
        _donate(donor1, pool, USDC_10K);

        uint256 totalClaimed = 0;
        uint256 perProof = USDC_10K / numProofs;

        for (uint256 i = 0; i < numProofs; i++) {
            uint256 claimAmount = (i == uint256(numProofs) - 1)
                ? USDC_10K - totalClaimed  // last proof takes the remainder
                : perProof;

            _submitProof(pool, ngo1, "QmBatchCID", claimAmount);
            _releaseFunds(pool, i);
            totalClaimed += claimAmount;
        }

        assertEq(pool.totalReleased(), USDC_10K);
        assertEq(pool.getPoolBalance(), 0);
        assertEq(sbt.tokenCount(), numProofs);
    }

    // =====================================================================
    //  Fuzz: Release fails if claim > balance
    // =====================================================================

    function testFuzz_release_revertsIfClaimExceedsBalance(
        uint256 donateAmount,
        uint256 claimAmount
    ) public {
        donateAmount = bound(donateAmount, 1, USDC_10K - 1);
        claimAmount = bound(claimAmount, donateAmount + 1, type(uint128).max);

        _donate(donor1, pool, donateAmount);
        _submitProof(pool, ngo1, "QmCID", claimAmount);

        vm.prank(multiSig);
        vm.expectRevert(
            abi.encodeWithSelector(
                FundPool.InsufficientPoolBalance.selector,
                claimAmount,
                donateAmount
            )
        );
        pool.releaseFunds(0);
    }
}
