// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {FundPool} from "../src/FundPool.sol";

/// @notice Release funds for a given proof ID (multi-sig only).
/// Env vars: POOL, PROOF_ID
contract ReleaseFunds is Script {
    function run() external {
        address pool    = vm.envAddress("POOL");
        uint256 proofId = vm.envUint("PROOF_ID");

        vm.startBroadcast();
        FundPool(pool).releaseFunds(proofId);
        vm.stopBroadcast();

        console.log("Funds released for proofId", proofId, "on pool", pool);
    }
}
