// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {FundPool} from "../src/FundPool.sol";

/// @notice Resume donations on a FundPool (multi-sig only).
/// Env vars: POOL
contract ResumeDonations is Script {
    function run() external {
        address pool = vm.envAddress("POOL");

        vm.startBroadcast();
        FundPool(pool).resumeDonations();
        vm.stopBroadcast();

        console.log("Donations resumed on pool", pool);
    }
}
