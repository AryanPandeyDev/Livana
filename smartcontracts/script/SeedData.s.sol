// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {Script, console} from "forge-std/Script.sol";
import {MockUSDC} from "../src/mocks/MockUSDC.sol";
import {PoolFactory} from "../src/PoolFactory.sol";

import {FundPool} from "../src/FundPool.sol";

/**
 * @title SeedData
 * @notice Full deployment + data seeding script for local Anvil development.
 *
 * What it does (in order):
 *   1. Deploy MockUSDC
 *   2. Deploy PoolFactory (which deploys LivanaSBT internally)
 *   3. Set the admin (anvil account[0]) as multiSigAdmin
 *   4. Verify 3 NGO addresses
 *   5. Each NGO deploys their own FundPool (3 pools total)
 *   6. Mint MockUSDC to 3 donor addresses
 *   7. Each donor approves + donates to all 3 pools
 *   8. Write all deployed addresses to seed-data.json (read by backend / frontend)
 *
 * Usage:
 *   forge script script/SeedData.s.sol \
 *     --rpc-url http://127.0.0.1:8545 \
 *     --broadcast \
 *     --private-key <ANVIL_ADMIN_PK>
 *
 *   Or via Makefile:  make seed
 */
contract SeedData is Script {
    // -------------------------------------------------------------------------
    // Anvil default accounts (deterministic)
    // -------------------------------------------------------------------------
    address constant ADMIN    = 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266;
    address constant VERIFIER = 0x70997970C51812dc3A010C7d01b50e0d17dc79C8;
    address constant DONOR1   = 0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC;
    address constant DONOR2   = 0x90F79bf6EB2c4f870365E785982E1f101E93b906;
    address constant DONOR3   = 0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65;
    address constant NGO1     = 0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc;
    address constant NGO2     = 0x976EA74026E726554dB657fA54763abd0C3a0aa9;
    address constant NGO3     = 0x14dC79964da2C08b23698B3D3cc7Ca32193d9955;

    // Anvil private keys for the accounts that need to sign transactions
    uint256 constant ADMIN_PK    = 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80;
    uint256 constant VERIFIER_PK = 0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d;
    uint256 constant DONOR1_PK   = 0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a;
    uint256 constant DONOR2_PK   = 0x7c852118294e51e653712a81e05800f419141751be58f605c371e15141b007a6;
    uint256 constant DONOR3_PK   = 0x47e179ec197488593b187f80a00eb0da91f1b9d0b13f8733639f19c30a34926a;
    uint256 constant NGO1_PK     = 0x8b3a350cf5c34c9194ca85829a2df0ec3153be0318b5e2d3348e872092edffba;
    uint256 constant NGO2_PK     = 0x92db14e403b83dfe3df233f83dfa3a0d7096f21ca9b0d6d6b8d88b2b4ec1564e;
    uint256 constant NGO3_PK     = 0x4bbbf85ce3377467afe5d46f804f221813b2bb87f24d81f60f1fcdbf7cbf4356;

    // Seed amounts (USDC has 6 decimals)
    uint256 constant MINT_PER_DONOR   = 10_000e6;  // 10,000 USDC each
    uint256 constant DONATE_PER_CALL  =  1_000e6;  // 1,000 USDC per donation

    function run() external {
        // -------------------------------------------------------------------------
        // Step 1: Admin deploys MockUSDC + PoolFactory
        // -------------------------------------------------------------------------
        vm.startBroadcast(ADMIN_PK);

        MockUSDC usdc = new MockUSDC();
        PoolFactory factory = new PoolFactory(address(usdc));

        // Set admin as multiSigAdmin (in prod this would be a Gnosis Safe)
        factory.setMultiSigAdmin(ADMIN);

        // -------------------------------------------------------------------------
        // Step 2: Admin verifies NGOs
        // -------------------------------------------------------------------------
        factory.addVerifiedNGO(NGO1);
        factory.addVerifiedNGO(NGO2);
        factory.addVerifiedNGO(NGO3);

        vm.stopBroadcast();

        // -------------------------------------------------------------------------
        // Step 3: Each NGO deploys their own pool
        // -------------------------------------------------------------------------
        vm.startBroadcast(NGO1_PK);
        address pool1 = factory.deployPool("QmSeedPool1Metadata");
        vm.stopBroadcast();

        vm.startBroadcast(NGO2_PK);
        address pool2 = factory.deployPool("QmSeedPool2Metadata");
        vm.stopBroadcast();

        vm.startBroadcast(NGO3_PK);
        address pool3 = factory.deployPool("QmSeedPool3Metadata");
        vm.stopBroadcast();

        // -------------------------------------------------------------------------
        // Step 4: Admin mints USDC to donors
        // -------------------------------------------------------------------------
        vm.startBroadcast(ADMIN_PK);
        usdc.mint(DONOR1, MINT_PER_DONOR);
        usdc.mint(DONOR2, MINT_PER_DONOR);
        usdc.mint(DONOR3, MINT_PER_DONOR);
        vm.stopBroadcast();

        // -------------------------------------------------------------------------
        // Step 5: Donors approve & donate to all pools
        // -------------------------------------------------------------------------
        _donateFromAccount(DONOR1_PK, address(usdc), pool1, pool2, pool3);
        _donateFromAccount(DONOR2_PK, address(usdc), pool1, pool2, pool3);
        _donateFromAccount(DONOR3_PK, address(usdc), pool1, pool2, pool3);

        // -------------------------------------------------------------------------
        // Step 6: Write seed-data.json (using vm.writeJson)
        // -------------------------------------------------------------------------
        string memory json = "output";
        vm.serializeAddress(json, "admin",       ADMIN);
        vm.serializeAddress(json, "verifier",    VERIFIER);
        vm.serializeAddress(json, "donor1",      DONOR1);
        vm.serializeAddress(json, "donor2",      DONOR2);
        vm.serializeAddress(json, "donor3",      DONOR3);
        vm.serializeAddress(json, "ngo1",        NGO1);
        vm.serializeAddress(json, "ngo2",        NGO2);
        vm.serializeAddress(json, "ngo3",        NGO3);
        vm.serializeAddress(json, "mockUSDC",    address(usdc));
        vm.serializeAddress(json, "poolFactory", address(factory));
        vm.serializeAddress(json, "sbt",         address(factory.sbt()));
        vm.serializeAddress(json, "pool1",       pool1);
        vm.serializeAddress(json, "pool2",       pool2);
        string memory finalJson = vm.serializeAddress(json, "pool3", pool3);

        vm.writeJson(finalJson, "./seed-data.json");

        // -------------------------------------------------------------------------
        // Summary
        // -------------------------------------------------------------------------
        console.log("=== Livana Seed Complete ===");
        console.log("MockUSDC    :", address(usdc));
        console.log("PoolFactory :", address(factory));
        console.log("SBT         :", address(factory.sbt()));
        console.log("Pool1 (NGO1):", pool1);
        console.log("Pool2 (NGO2):", pool2);
        console.log("Pool3 (NGO3):", pool3);
        console.log("seed-data.json written.");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    function _donateFromAccount(
        uint256 pk,
        address usdcAddr,
        address pool1,
        address pool2,
        address pool3
    ) internal {
        vm.startBroadcast(pk);
        MockUSDC usdc = MockUSDC(usdcAddr);

        usdc.approve(pool1, DONATE_PER_CALL);
        FundPool(pool1).donate(DONATE_PER_CALL);

        usdc.approve(pool2, DONATE_PER_CALL);
        FundPool(pool2).donate(DONATE_PER_CALL);

        usdc.approve(pool3, DONATE_PER_CALL);
        FundPool(pool3).donate(DONATE_PER_CALL);

        vm.stopBroadcast();
    }
}
