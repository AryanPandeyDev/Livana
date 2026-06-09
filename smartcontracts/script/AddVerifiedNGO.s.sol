// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {PoolFactory} from "../src/PoolFactory.sol";

/// @notice Whitelist an NGO in PoolFactory.
/// Env vars: FACTORY, NGO
contract AddVerifiedNGO is Script {
    function run() external {
        address factory = vm.envAddress("FACTORY");
        address ngo     = vm.envAddress("NGO");

        vm.startBroadcast();
        PoolFactory(factory).addVerifiedNGO(ngo);
        vm.stopBroadcast();

        console.log("NGO verified:", ngo);
    }
}
