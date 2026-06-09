// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {FundPool} from "../src/FundPool.sol";


/// @notice Read-only query of a FundPool's state.
/// Env vars: POOL
contract QueryPool is Script {
    function run() external view {
        address poolAddr = vm.envAddress("POOL");
        FundPool pool    = FundPool(poolAddr);

        uint256 balance = pool.getPoolBalance();

        console.log("=== FundPool ===");
        console.log("Address      :", poolAddr);
        console.log("Creator      :", pool.creator());
        console.log("MultiSig     :", pool.multiSigAdmin());
        console.log("USDC balance :", balance);
        console.log("totalDonated :", pool.totalDonated());
        console.log("totalReleased:", pool.totalReleased());
        console.log("proofCount   :", pool.proofCount());
    }
}
