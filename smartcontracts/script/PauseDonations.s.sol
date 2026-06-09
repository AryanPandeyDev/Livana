// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {FundPool} from "../src/FundPool.sol";

/// @notice Pause donations on a FundPool (multi-sig only).
/// Env vars: POOL
contract PauseDonations is Script {
    function run() external {
        address pool = vm.envAddress("POOL");

        vm.startBroadcast();
        FundPool(pool).pauseDonations();
        vm.stopBroadcast();

        console.log("Donations paused on pool", pool);
    }
}
