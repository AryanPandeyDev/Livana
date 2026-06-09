// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {BaseTest} from "../helpers/BaseTest.sol";
import {PoolFactory} from "../../src/PoolFactory.sol";
import {FundPool} from "../../src/FundPool.sol";

/**
 * @title PoolFactoryTest
 * @notice Unit tests for PoolFactory: constructor, setMultiSigAdmin, NGO
 *         registry, and pool deployment.
 */
contract PoolFactoryTest is BaseTest {
    // =====================================================================
    //  Constructor
    // =====================================================================

    function test_constructor_setsUSDC() public view {
        assertEq(address(factory.usdc()), address(usdc));
    }

    function test_constructor_deploysSBT() public view {
        assertTrue(address(sbt) != address(0));
        assertEq(address(sbt.factory()), address(factory));
    }

    function test_constructor_revertsOnZeroUSDC() public {
        vm.expectRevert(PoolFactory.ZeroAddress.selector);
        new PoolFactory(address(0));
    }

    // =====================================================================
    //  setMultiSigAdmin
    // =====================================================================

    function test_setMultiSigAdmin_setsCorrectly() public view {
        assertEq(factory.multiSigAdmin(), multiSig);
    }

    function test_setMultiSigAdmin_revertsOnSecondCall() public {
        vm.expectRevert(PoolFactory.MultiSigAlreadySet.selector);
        factory.setMultiSigAdmin(makeAddr("newMultiSig"));
    }

    function test_setMultiSigAdmin_revertsOnZeroAddress() public {
        // Deploy fresh factory without multi-sig set
        PoolFactory freshFactory = new PoolFactory(address(usdc));
        vm.expectRevert(PoolFactory.ZeroAddress.selector);
        freshFactory.setMultiSigAdmin(address(0));
    }

    function test_setMultiSigAdmin_emitsEvent() public {
        PoolFactory freshFactory = new PoolFactory(address(usdc));
        address newMultiSig = makeAddr("newMultiSig");
        vm.expectEmit(true, false, false, false);
        emit PoolFactory.MultiSigAdminSet(newMultiSig);
        freshFactory.setMultiSigAdmin(newMultiSig);
    }

    // =====================================================================
    //  addVerifiedNGO
    // =====================================================================

    function test_addVerifiedNGO_works() public {
        _verifyNGO(ngo1);
        assertTrue(factory.isVerified(ngo1));
    }

    function test_addVerifiedNGO_emitsEvent() public {
        vm.prank(multiSig);
        vm.expectEmit(true, false, false, false);
        emit PoolFactory.NGOApproved(ngo1);
        factory.addVerifiedNGO(ngo1);
    }

    function test_addVerifiedNGO_revertsNotMultiSig() public {
        vm.prank(stranger);
        vm.expectRevert(PoolFactory.NotMultiSig.selector);
        factory.addVerifiedNGO(ngo1);
    }

    function test_addVerifiedNGO_revertsOnZeroAddress() public {
        vm.prank(multiSig);
        vm.expectRevert(PoolFactory.ZeroAddress.selector);
        factory.addVerifiedNGO(address(0));
    }

    function test_addVerifiedNGO_revertsOnDuplicate() public {
        _verifyNGO(ngo1);
        vm.prank(multiSig);
        vm.expectRevert(PoolFactory.AlreadyVerified.selector);
        factory.addVerifiedNGO(ngo1);
    }

    // =====================================================================
    //  revokeNGO
    // =====================================================================

    function test_revokeNGO_works() public {
        _verifyNGO(ngo1);
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);
        assertFalse(factory.isVerified(ngo1));
    }

    function test_revokeNGO_emitsEvent() public {
        _verifyNGO(ngo1);
        vm.prank(multiSig);
        vm.expectEmit(true, false, false, false);
        emit PoolFactory.NGORevoked(ngo1);
        factory.revokeNGO(ngo1);
    }

    function test_revokeNGO_revertsNotMultiSig() public {
        _verifyNGO(ngo1);
        vm.prank(stranger);
        vm.expectRevert(PoolFactory.NotMultiSig.selector);
        factory.revokeNGO(ngo1);
    }

    function test_revokeNGO_revertsIfNotVerified() public {
        vm.prank(multiSig);
        vm.expectRevert(PoolFactory.NotCurrentlyVerified.selector);
        factory.revokeNGO(ngo1);
    }

    function test_revokeNGO_canReverifyAfterRevoke() public {
        _verifyNGO(ngo1);
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);
        // Can re-verify
        _verifyNGO(ngo1);
        assertTrue(factory.isVerified(ngo1));
    }

    // =====================================================================
    //  deployPool
    // =====================================================================

    function test_deployPool_works() public {
        _verifyNGO(ngo1);
        vm.prank(ngo1);
        address poolAddr = factory.deployPool(DEFAULT_METADATA_CID);

        assertTrue(factory.isPool(poolAddr));
        assertEq(factory.poolCount(), 1);
    }

    function test_deployPool_storesMetadataCid() public {
        _verifyNGO(ngo1);
        vm.prank(ngo1);
        address poolAddr = factory.deployPool("QmCustomCID");
        assertEq(FundPool(poolAddr).metadataCid(), "QmCustomCID");
    }

    function test_deployPool_emitsEvent() public {
        _verifyNGO(ngo1);
        vm.prank(ngo1);
        // We don't know the exact pool address yet, so we check indexed fields
        vm.expectEmit(false, true, true, false);
        emit PoolFactory.PoolDeployed(address(0), ngo1, 0, DEFAULT_METADATA_CID);
        factory.deployPool(DEFAULT_METADATA_CID);
    }

    function test_deployPool_incrementsPoolCount() public {
        _verifyNGO(ngo1);
        vm.prank(ngo1);
        factory.deployPool(DEFAULT_METADATA_CID);
        vm.prank(ngo1);
        factory.deployPool("QmSecondPool");
        assertEq(factory.poolCount(), 2);
    }

    function test_deployPool_revertsIfNotVerified() public {
        vm.prank(stranger);
        vm.expectRevert(PoolFactory.NotVerifiedNGO.selector);
        factory.deployPool(DEFAULT_METADATA_CID);
    }

    function test_deployPool_revertsIfMultiSigNotSet() public {
        PoolFactory freshFactory = new PoolFactory(address(usdc));
        // No setMultiSigAdmin called — cannot verify NGOs directly either
        // but let's simulate by calling from a non-existent multiSig
        vm.prank(ngo1);
        vm.expectRevert(PoolFactory.NotVerifiedNGO.selector);
        freshFactory.deployPool(DEFAULT_METADATA_CID);
    }

    function test_deployPool_revertsAfterRevocation() public {
        _verifyNGO(ngo1);
        vm.prank(multiSig);
        factory.revokeNGO(ngo1);
        vm.prank(ngo1);
        vm.expectRevert(PoolFactory.NotVerifiedNGO.selector);
        factory.deployPool(DEFAULT_METADATA_CID);
    }

    function test_deployPool_revertsOnEmptyCID() public {
        _verifyNGO(ngo1);
        vm.prank(ngo1);
        vm.expectRevert(PoolFactory.EmptyCID.selector);
        factory.deployPool("");
    }

    // =====================================================================
    //  isVerified / isPool — edge cases
    // =====================================================================

    function test_isVerified_returnsFalseByDefault() public view {
        assertFalse(factory.isVerified(stranger));
    }

    function test_isPool_returnsFalseByDefault() public view {
        assertFalse(factory.isPool(stranger));
    }
}
