// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Test} from "forge-std/Test.sol";
import {DeployLivana} from "../../script/DeployLivana.s.sol";
import {MockUSDC} from "../../src/mocks/MockUSDC.sol";
import {PoolFactory} from "../../src/PoolFactory.sol";
import {LivanaSBT} from "../../src/LivanaSBT.sol";
import {FundPool} from "../../src/FundPool.sol";

/**
 * @title BaseTest
 * @notice Shared test harness used by all test files.
 *         Uses the DeployLivana script so tests mirror production deployment.
 */
abstract contract BaseTest is Test {
    // --- Contracts ---
    DeployLivana deployer;
    MockUSDC usdc;
    PoolFactory factory;
    LivanaSBT sbt;

    // --- Actors ---
    address multiSig = makeAddr("multiSig");
    address ngo1 = makeAddr("ngo1");
    address ngo2 = makeAddr("ngo2");
    address donor1 = makeAddr("donor1");
    address donor2 = makeAddr("donor2");
    address stranger = makeAddr("stranger");

    // --- Constants ---
    uint256 constant USDC_1 = 1e6;       // 1 USDC
    uint256 constant USDC_100 = 100e6;   // 100 USDC
    uint256 constant USDC_1000 = 1000e6; // 1,000 USDC
    uint256 constant USDC_10K = 10_000e6;

    string constant DEFAULT_METADATA_CID = "QmTestPoolMetadata123";

    function setUp() public virtual {
        deployer = new DeployLivana();
        DeployLivana.Deployment memory d = deployer.deployWithMockUSDC(multiSig);

        usdc = d.usdc;
        factory = d.factory;
        sbt = d.sbt;

        // Fund donors with USDC
        usdc.mint(donor1, USDC_10K);
        usdc.mint(donor2, USDC_10K);
    }

    // --- Helpers ---

    /// @dev Verify an NGO via the multi-sig.
    function _verifyNGO(address _ngo) internal {
        vm.prank(multiSig);
        factory.addVerifiedNGO(_ngo);
    }

    /// @dev Deploy a pool for an NGO (must be verified first).
    function _deployPool(address _ngo) internal returns (FundPool) {
        return _deployPoolWithCid(_ngo, DEFAULT_METADATA_CID);
    }

    /// @dev Deploy a pool with a specific metadata CID.
    function _deployPoolWithCid(address _ngo, string memory _cid) internal returns (FundPool) {
        vm.prank(_ngo);
        address poolAddr = factory.deployPool(_cid);
        return FundPool(poolAddr);
    }

    /// @dev Verify an NGO and deploy a pool in one step.
    function _verifyAndDeployPool(address _ngo) internal returns (FundPool) {
        _verifyNGO(_ngo);
        return _deployPool(_ngo);
    }

    /// @dev Donate USDC from a donor to a pool (handles approval).
    function _donate(address _donor, FundPool _pool, uint256 _amount) internal {
        vm.startPrank(_donor);
        usdc.approve(address(_pool), _amount);
        _pool.donate(_amount);
        vm.stopPrank();
    }

    /// @dev Submit a proof from the NGO creator.
    function _submitProof(
        FundPool _pool,
        address _ngo,
        string memory _cid,
        uint256 _amount
    ) internal returns (uint256) {
        vm.prank(_ngo);
        return _pool.submitProof(_cid, _amount);
    }

    /// @dev Release funds for a proof via multi-sig.
    function _releaseFunds(FundPool _pool, uint256 _proofId) internal {
        vm.prank(multiSig);
        _pool.releaseFunds(_proofId);
    }
}
