// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script} from "forge-std/Script.sol";
import {MockUSDC} from "../src/mocks/MockUSDC.sol";
import {PoolFactory} from "../src/PoolFactory.sol";
import {LivanaSBT} from "../src/LivanaSBT.sol";

/**
 * @title DeployLivana
 * @notice Deployment script for the full Livana protocol.
 *         Used by both tests (via run()) and real deployments (via forge script).
 *
 * Deployment order:
 *   1. MockUSDC (testnet) or use existing USDC address (mainnet)
 *   2. PoolFactory — which deploys LivanaSBT in its constructor
 *   3. Set the Safe multi-sig wallet address on PoolFactory
 */
contract DeployLivana is Script {
    struct Deployment {
        MockUSDC usdc;
        PoolFactory factory;
        LivanaSBT sbt;
    }

    /**
     * @notice Deploy using a mock USDC token.
     *         Used in testing and local/testnet environments.
     * @param _multiSigAdmin Address of the Safe multi-sig wallet.
     * @return d Struct containing all deployed contracts.
     */
    function deployWithMockUSDC(address _multiSigAdmin)
        public
        returns (Deployment memory d)
    {
        MockUSDC usdc = new MockUSDC();
        d = deploy(address(usdc), _multiSigAdmin);
        d.usdc = usdc;
    }

    /**
     * @notice Deploy with an existing USDC address.
     *         Used in production (mainnet/polygon) where USDC is already deployed.
     * @param _usdc          Existing USDC token address.
     * @param _multiSigAdmin Address of the Safe multi-sig wallet.
     * @return d Struct containing all deployed contracts.
     */
    function deployWithExistingUSDC(address _usdc, address _multiSigAdmin)
        public
        returns (Deployment memory d)
    {
        d = deploy(_usdc, _multiSigAdmin);
    }

    function deploy(address _usdc, address _multiSigAdmin)
        internal
        returns (Deployment memory d)
    {
        // 1. Deploy PoolFactory (also deploys LivanaSBT internally)
        PoolFactory factory = new PoolFactory(_usdc);

        // 2. Wire up the multi-sig
        factory.setMultiSigAdmin(_multiSigAdmin);

        // 3. Package return values
        d.factory = factory;
        d.sbt = factory.sbt();
    }

    /// @dev Entry point for `forge script` CLI usage.
    function run() external {
        address multiSig = vm.envAddress("MULTISIG_ADDRESS");
        address usdc = vm.envOr("USDC_ADDRESS", address(0));

        vm.startBroadcast();

        if (usdc == address(0)) {
            deployWithMockUSDC(multiSig);
        } else {
            deployWithExistingUSDC(usdc, multiSig);
        }

        vm.stopBroadcast();
    }
}
