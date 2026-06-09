// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {PoolFactory} from "../src/PoolFactory.sol";

/// @notice Check whether an address is a verified NGO.
/// Env vars: FACTORY, NGO
contract CheckNGOVerified is Script {
    function run() external view {
        address factory = vm.envAddress("FACTORY");
        address ngo     = vm.envAddress("NGO");

        bool verified = PoolFactory(factory).isVerified(ngo);
        console.log("NGO        :", ngo);
        console.log("isVerified :", verified);
    }
}
