package com.livana.backend.indexer.service;

import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;

import com.livana.backend.pool.entity.Pool;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared test factory for building Web3j Log objects that mimic on-chain events.
 *
 * Centralizes ABI encoding logic so individual test files don't duplicate
 * the tricky parts (dynamic string offsets, topic padding, etc.).
 *
 * Usage: all methods are static. Import statically or call directly.
 */
final class IndexerLogTestFactory {

    private IndexerLogTestFactory() {} // utility class

    private static final long DEFAULT_BLOCK_NUM = 1000L;

    // ========================================================================
    // Log builders
    // ========================================================================

    /**
     * DonationReceived(address indexed donor, uint256 amount)
     */
    static Log donationReceived(String txHash, int logIndex,
                                String poolAddress, String donorAddress, BigInteger amount) {
        Log log = baseLog(txHash, logIndex, poolAddress, DEFAULT_BLOCK_NUM);
        log.setTopics(List.of(
                EventDefinitions.DONATION_RECEIVED_SIG,
                padAddressToTopic(donorAddress)
        ));
        log.setData("0x" + TypeEncoder.encode(new Uint256(amount)));
        return log;
    }

    /**
     * PoolDeployed(address indexed poolAddress, address indexed creator,
     *              uint256 indexed poolIndex, string metadataCid)
     */
    static Log poolDeployed(String txHash, int logIndex,
                            String poolAddress, String creatorAddress,
                            BigInteger poolIndex, String metadataCid, long blockNumber) {
        Log log = baseLog(txHash, logIndex,
                "0x0000000000000000000000000000000000facade", blockNumber); // factory address
        log.setTopics(List.of(
                EventDefinitions.POOL_DEPLOYED_SIG,
                padAddressToTopic(poolAddress),
                padAddressToTopic(creatorAddress),
                "0x" + TypeEncoder.encode(new Uint256(poolIndex))
        ));
        // Dynamic string: offset (0x20) + length + padded data
        String dynamicPart = TypeEncoder.encode(new Utf8String(metadataCid));
        String offset = TypeEncoder.encode(new Uint256(BigInteger.valueOf(32)));
        log.setData("0x" + offset + dynamicPart);
        return log;
    }

    /**
     * ProofSubmitted(uint256 indexed proofId, string ipfsCid, uint256 amount)
     */
    static Log proofSubmitted(String txHash, int logIndex, String poolAddress,
                              BigInteger proofId, String ipfsCid, BigInteger amount,
                              long blockNumber) {
        Log log = baseLog(txHash, logIndex, poolAddress, blockNumber);
        log.setTopics(List.of(
                EventDefinitions.PROOF_SUBMITTED_SIG,
                padUint256ToTopic(proofId)
        ));
        // Non-indexed: (string ipfsCid, uint256 amount)
        // Head: offset to string (0x40 = 64, two head slots) + amount (static)
        // Tail: string encoding (length + padded data)
        String offsetToString = TypeEncoder.encode(new Uint256(BigInteger.valueOf(64)));
        String encodedAmount = TypeEncoder.encode(new Uint256(amount));
        String encodedString = TypeEncoder.encode(new Utf8String(ipfsCid));
        log.setData("0x" + offsetToString + encodedAmount + encodedString);
        return log;
    }

    /**
     * FundsReleased(uint256 indexed proofId, address indexed ngo, uint256 amount)
     */
    static Log fundsReleased(String txHash, int logIndex, String poolAddress,
                             BigInteger proofId, String ngoAddress, BigInteger amount,
                             long blockNumber) {
        Log log = baseLog(txHash, logIndex, poolAddress, blockNumber);
        log.setTopics(List.of(
                EventDefinitions.FUNDS_RELEASED_SIG,
                padUint256ToTopic(proofId),
                padAddressToTopic(ngoAddress)
        ));
        log.setData("0x" + TypeEncoder.encode(new Uint256(amount)));
        return log;
    }

    /**
     * NGOApproved(address indexed ngo)
     */
    static Log ngoApproved(String txHash, int logIndex,
                           String contractAddress, String ngoAddress) {
        Log log = baseLog(txHash, logIndex, contractAddress, DEFAULT_BLOCK_NUM);
        log.setTopics(List.of(
                EventDefinitions.NGO_APPROVED_SIG,
                padAddressToTopic(ngoAddress)
        ));
        log.setData("0x");
        return log;
    }

    /**
     * NGORevoked(address indexed ngo)
     */
    static Log ngoRevoked(String txHash, int logIndex,
                          String contractAddress, String ngoAddress, long blockNumber) {
        Log log = baseLog(txHash, logIndex, contractAddress, blockNumber);
        log.setTopics(List.of(
                EventDefinitions.NGO_REVOKED_SIG,
                padAddressToTopic(ngoAddress)
        ));
        log.setData("0x");
        return log;
    }

    /**
     * Paused(address account) — non-indexed
     */
    static Log paused(String txHash, int logIndex,
                      String contractAddress, String accountAddress, long blockNumber) {
        Log log = baseLog(txHash, logIndex, contractAddress, blockNumber);
        log.setTopics(List.of(EventDefinitions.PAUSED_SIG));
        log.setData("0x" + TypeEncoder.encode(new Address(accountAddress)));
        return log;
    }

    /**
     * Unpaused(address account) — non-indexed
     */
    static Log unpaused(String txHash, int logIndex,
                        String contractAddress, String accountAddress, long blockNumber) {
        Log log = baseLog(txHash, logIndex, contractAddress, blockNumber);
        log.setTopics(List.of(EventDefinitions.UNPAUSED_SIG));
        log.setData("0x" + TypeEncoder.encode(new Address(accountAddress)));
        return log;
    }

    /**
     * Unknown event with a bogus topic0.
     */
    static Log unknownEvent(String txHash, int logIndex) {
        Log log = baseLog(txHash, logIndex,
                "0x0000000000000000000000000000000000000001", DEFAULT_BLOCK_NUM);
        log.setTopics(List.of(
                "0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
        ));
        log.setData("0x");
        return log;
    }

    // ========================================================================
    // Entity builders
    // ========================================================================

    /**
     * Build a Pool entity with configurable address, totalDonated, totalReleased, isPaused.
     */
    static Pool pool(String onChainAddress, long totalDonated, long totalReleased, boolean isPaused) {
        return Pool.builder()
                .onChainAddress(onChainAddress)
                .creatorAddress("0x0000000000000000000000000000000000000099")
                .poolIndex(0)
                .metadataCid("QmTest")
                .title("Test Pool")
                .description("Test")
                .region("Global")
                .targetAmount(1_000_000_000L)
                .totalDonated(totalDonated)
                .totalReleased(totalReleased)
                .isPaused(isPaused)
                .deployTxHash("0x" + "aa".repeat(32))
                .deployBlock(999L)
                .deployedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    // ========================================================================
    // Encoding helpers
    // ========================================================================

    /**
     * Pad a 0x-prefixed address to 32-byte topic format (left-pad with zeros).
     * E.g. 0x1111...1111 → 0x0000000000000000000000001111...1111
     */
    static String padAddressToTopic(String address) {
        String stripped = address.startsWith("0x") ? address.substring(2) : address;
        return "0x" + "0".repeat(64 - stripped.length()) + stripped;
    }

    /**
     * Pad a uint256 value to 32-byte topic format (left-pad with zeros).
     */
    static String padUint256ToTopic(BigInteger value) {
        String hex = value.toString(16);
        return "0x" + "0".repeat(64 - hex.length()) + hex;
    }

    // ========================================================================
    // Internal
    // ========================================================================

    private static Log baseLog(String txHash, int logIndex, String address, long blockNumber) {
        Log log = new Log();
        log.setTransactionHash(txHash);
        log.setLogIndex(BigInteger.valueOf(logIndex).toString(16));
        log.setAddress(address);
        log.setBlockNumber("0x" + BigInteger.valueOf(blockNumber).toString(16));
        return log;
    }
}
