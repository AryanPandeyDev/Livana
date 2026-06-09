// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {MockUSDC} from "../src/mocks/MockUSDC.sol";

/// @notice Mint MockUSDC tokens to a recipient.
/// Env vars: USDC, RECIPIENT, AMOUNT
contract MintMockUSDC is Script {
    function run() external {
        address usdc      = vm.envAddress("USDC");
        address recipient = vm.envAddress("RECIPIENT");
        uint256 amount    = vm.envUint("AMOUNT");

        vm.startBroadcast();
        MockUSDC(usdc).mint(recipient, amount);
        vm.stopBroadcast();

        console.log("Minted", amount, "USDC to", recipient);
    }
}
