// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {PoolFactory} from "../src/PoolFactory.sol";

/// @notice Revoke a previously verified NGO from PoolFactory.
/// Env vars: FACTORY, NGO
contract RevokeNGO is Script {
    function run() external {
        address factory = vm.envAddress("FACTORY");
        address ngo     = vm.envAddress("NGO");

        vm.startBroadcast();
        PoolFactory(factory).revokeNGO(ngo);
        vm.stopBroadcast();

        console.log("NGO revoked:", ngo);
    }
}
