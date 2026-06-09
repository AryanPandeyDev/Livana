// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {FundPool} from "../src/FundPool.sol";

/// @notice Donate USDC to a FundPool (broadcaster must have approved first).
/// Env vars: POOL, AMOUNT
contract Donate is Script {
    function run() external {
        address pool   = vm.envAddress("POOL");
        uint256 amount = vm.envUint("AMOUNT");

        vm.startBroadcast();
        FundPool(pool).donate(amount);
        vm.stopBroadcast();

        console.log("Donated", amount, "USDC to pool", pool);
    }
}
