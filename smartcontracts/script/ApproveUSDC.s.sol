// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/// @notice Approve a spender (e.g. a FundPool) to spend USDC on behalf of broadcaster.
/// Env vars: USDC, SPENDER, AMOUNT
contract ApproveUSDC is Script {
    function run() external {
        address usdc    = vm.envAddress("USDC");
        address spender = vm.envAddress("SPENDER");
        uint256 amount  = vm.envUint("AMOUNT");

        vm.startBroadcast();
        IERC20(usdc).approve(spender, amount);
        vm.stopBroadcast();

        console.log("Approved", amount, "USDC for spender", spender);
    }
}
