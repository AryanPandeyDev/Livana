package com.livana.backend.indexer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the blockchain event indexer.
 *
 * Properties are under the livana.indexer.* namespace.
 * Example:
 *   livana.indexer.rpc-url=http://127.0.0.1:8545
 *   livana.indexer.factory-address=0x...
 *   livana.indexer.sbt-address=0x...
 */
@Configuration
@ConfigurationProperties(prefix = "livana.indexer")
@Getter
@Setter
public class IndexerProperties {

    /**
     * JSON-RPC URL of the Ethereum/Avalanche node.
     * For local dev: http://127.0.0.1:8545 (Anvil)
     */
    private String rpcUrl = "http://127.0.0.1:8545";

    /**
     * PoolFactory contract address. Required when the indexer is enabled.
     */
    private String factoryAddress;

    /**
     * LivanaSBT contract address.
     * Optional — if not set, the indexer will call factory.sbt() on-chain
     * to discover it at startup. Setting this explicitly avoids an extra RPC call.
     */
    private String sbtAddress;

    /**
     * Polling interval in milliseconds. Default: 3000ms (3 seconds).
     * Avalanche C-Chain has ~2s block times, so 3s is near real-time.
     */
    private long pollIntervalMs = 3000;

    /**
     * Maximum number of blocks to query per eth_getLogs call.
     * Most RPC providers limit this to 2000-10000 blocks.
     */
    private long batchSize = 2000;

    /**
     * Number of confirmation blocks before treating events as final.
     * Provides a buffer against chain reorganizations.
     *
     * Recommended values:
     *   - 0: Local dev with Anvil (instant finality, no reorgs)
     *   - 1-2: Avalanche C-Chain (sub-second finality, reorgs extremely rare)
     *   - 12-15: Ethereum mainnet (probabilistic finality)
     *
     * Default is 0 for local dev. Override via INDEXER_CONFIRMATIONS for production.
     */
    private long confirmations = 0;

    /**
     * IPFS gateway URL for fetching pool metadata.
     */
    private String ipfsGatewayUrl = "https://gateway.pinata.cloud/ipfs/";

    /**
     * Whether the indexer is enabled. Set to false to disable polling.
     * Default: true. The main application.properties overrides to false
     * via INDEXER_ENABLED so the app starts cleanly without a blockchain node.
     */
    private boolean enabled = true;
}
