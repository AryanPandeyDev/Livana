// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {PoolFactory} from "../src/PoolFactory.sol";

/// @notice Read-only query of PoolFactory global state.
/// Env vars: FACTORY
contract QueryFactory is Script {
    function run() external view {
        address factoryAddr = vm.envAddress("FACTORY");
        PoolFactory factory  = PoolFactory(factoryAddr);

        console.log("=== PoolFactory ===");
        console.log("Address    :", factoryAddr);
        console.log("multiSig   :", factory.multiSigAdmin());
        console.log("USDC       :", address(factory.usdc()));
        console.log("SBT        :", address(factory.sbt()));
        console.log("Pool count :", factory.poolCount());
    }
}
