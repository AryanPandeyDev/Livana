// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {BaseTest} from "../helpers/BaseTest.sol";
import {FundPool} from "../../src/FundPool.sol";
import {LivanaSBT} from "../../src/LivanaSBT.sol";

/**
 * @title LivanaSBTTest
 * @notice Unit tests for LivanaSBT: minting, soulbound enforcement,
 *         EIP-5192 compliance, and access control.
 */
contract LivanaSBTTest is BaseTest {
    FundPool pool;

    function setUp() public override {
        super.setUp();
        pool = _verifyAndDeployPool(ngo1);
    }

    // =====================================================================
    //  Mint (via releaseFunds flow)
    // =====================================================================

    function test_mint_createsSBT() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        assertEq(sbt.tokenCount(), 1);
        assertEq(sbt.ownerOf(0), ngo1);
        assertEq(sbt.balanceOf(ngo1), 1);
    }

    function test_mint_storesReputationData() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        (address sbtPool, uint256 amount, uint256 ts) = sbt.getReputation(0);
        assertEq(sbtPool, address(pool));
        assertEq(amount, USDC_100);
        assertEq(ts, block.timestamp);
    }

    function test_mint_emitsLockedEvent() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);

        vm.prank(multiSig);
        vm.expectEmit(false, false, false, true, address(sbt));
        emit LivanaSBT.Locked(0);
        pool.releaseFunds(0);
    }

    function test_mint_incrementsTokenCount() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID1", USDC_100);
        _submitProof(pool, ngo1, "QmCID2", USDC_100);
        _releaseFunds(pool, 0);
        _releaseFunds(pool, 1);

        assertEq(sbt.tokenCount(), 2);
        assertEq(sbt.balanceOf(ngo1), 2);
    }

    function test_mint_revertsFromNonPool() public {
        vm.prank(stranger);
        vm.expectRevert(LivanaSBT.NotAPool.selector);
        sbt.mint(ngo1, USDC_100);
    }

    function test_mint_multipleNGOsGetSeparateSBTs() public {
        FundPool pool2 = _verifyAndDeployPool(ngo2);

        _donate(donor1, pool, USDC_1000);
        _donate(donor1, pool2, USDC_1000);

        _submitProof(pool, ngo1, "QmCID1", USDC_100);
        _submitProof(pool2, ngo2, "QmCID2", USDC_100);

        _releaseFunds(pool, 0);
        _releaseFunds(pool2, 0);

        assertEq(sbt.ownerOf(0), ngo1);
        assertEq(sbt.ownerOf(1), ngo2);
        assertEq(sbt.tokenCount(), 2);
    }

    // =====================================================================
    //  Soulbound Enforcement (transfer blocking)
    // =====================================================================

    function test_transfer_reverts() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        vm.prank(ngo1);
        vm.expectRevert(LivanaSBT.TransferBlocked.selector);
        sbt.transferFrom(ngo1, stranger, 0);
    }

    function test_safeTransfer_reverts() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        vm.prank(ngo1);
        vm.expectRevert(LivanaSBT.TransferBlocked.selector);
        sbt.safeTransferFrom(ngo1, stranger, 0);
    }

    function test_approve_then_transferFrom_reverts() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        // NGO approves stranger
        vm.prank(ngo1);
        sbt.approve(stranger, 0);

        // Stranger tries to transfer — blocked
        vm.prank(stranger);
        vm.expectRevert(LivanaSBT.TransferBlocked.selector);
        sbt.transferFrom(ngo1, stranger, 0);
    }

    // =====================================================================
    //  EIP-5192: locked()
    // =====================================================================

    function test_locked_returnsTrue() public {
        _donate(donor1, pool, USDC_1000);
        _submitProof(pool, ngo1, "QmCID", USDC_100);
        _releaseFunds(pool, 0);

        assertTrue(sbt.locked(0));
    }

    function test_locked_revertsForNonexistentToken() public {
        vm.expectRevert(LivanaSBT.TokenDoesNotExist.selector);
        sbt.locked(999);
    }

    // =====================================================================
    //  EIP-165: supportsInterface
    // =====================================================================

    function test_supportsInterface_EIP5192() public view {
        assertTrue(sbt.supportsInterface(0xb45a3c0e)); // EIP-5192
    }

    function test_supportsInterface_ERC721() public view {
        assertTrue(sbt.supportsInterface(0x80ac58cd)); // ERC-721
    }

    function test_supportsInterface_ERC721Metadata() public view {
        assertTrue(sbt.supportsInterface(0x5b5e139f)); // ERC-721 Metadata
    }

    function test_supportsInterface_ERC165() public view {
        assertTrue(sbt.supportsInterface(0x01ffc9a7)); // ERC-165
    }

    function test_supportsInterface_randomInterfaceReturnsFalse() public view {
        assertFalse(sbt.supportsInterface(0xdeadbeef));
    }

    // =====================================================================
    //  ERC721 Metadata
    // =====================================================================

    function test_name() public view {
        assertEq(sbt.name(), "Livana Reputation");
    }

    function test_symbol() public view {
        assertEq(sbt.symbol(), "LVNA");
    }

    // =====================================================================
    //  getReputation — edge cases
    // =====================================================================

    function test_getReputation_revertsForNonexistentToken() public {
        vm.expectRevert(LivanaSBT.TokenDoesNotExist.selector);
        sbt.getReputation(999);
    }
}
