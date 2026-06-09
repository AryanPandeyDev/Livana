package com.livana.backend.indexer.service;

import com.livana.backend.indexer.config.IndexerProperties;
import com.livana.backend.indexer.entity.IndexerState;
import com.livana.backend.indexer.repository.IndexerStateRepository;
import com.livana.backend.pool.repository.PoolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls the blockchain for new events using eth_getLogs.
 *
 * Architecture decision: HTTP polling over WebSocket subscriptions.
 *
 * HTTP polling is intentionally chosen over WebSocket for reliability:
 *   - Same code path for backfill (catchup on restart) and live indexing
 *   - No WebSocket reconnection / resubscription / gap-replay complexity
 *   - Works with any RPC provider (Anvil, Infura, Alchemy, QuickNode)
 *   - Avalanche C-Chain has ~2s block times — polling every 3s is near real-time
 *
 * From PRD:
 * - "Tracks lastIndexedBlock per contract for backfill on restart"
 * - "Dynamically subscribes to new FundPool contracts as PoolDeployed events arrive"
 * - "The event indexer must handle chain reorgs gracefully — store a small
 *    confirmation buffer before treating events as final"
 */
@Service
@Slf4j
public class BlockchainPollingService {

    private final Web3j web3j;
    private final IndexerProperties properties;
    private final IndexerStateRepository indexerStateRepository;
    private final PoolRepository poolRepository;
    private final EventPersistenceService eventPersistenceService;

    /**
     * Contracts being tracked: address → contract type (POOL_FACTORY, FUND_POOL, SBT).
     * Populated on startup and dynamically when new pools are discovered.
     */
    private final Map<String, String> trackedContracts = new ConcurrentHashMap<>();

    /**
     * Cache of block timestamps to avoid repeated eth_getBlockByNumber calls.
     */
    private final Map<Long, OffsetDateTime> blockTimestampCache = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    public BlockchainPollingService(
            Web3j web3j,
            IndexerProperties properties,
            IndexerStateRepository indexerStateRepository,
            PoolRepository poolRepository,
            EventPersistenceService eventPersistenceService) {
        this.web3j = web3j;
        this.properties = properties;
        this.indexerStateRepository = indexerStateRepository;
        this.poolRepository = poolRepository;
        this.eventPersistenceService = eventPersistenceService;
    }

    /**
     * Bootstrap the indexer on application startup.
     * Registers the factory, SBT, and all known pool contracts for tracking.
     *
     * SBT address resolution:
     *   1. If livana.indexer.sbt-address is set → use it
     *   2. Otherwise → call factory.sbt() on-chain to discover it
     *   3. If both fail → log an error and refuse to start (SBT indexing is required by PRD)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isEnabled()) {
            log.info("Event indexer is disabled (livana.indexer.enabled=false)");
            return;
        }

        String factoryAddress = properties.getFactoryAddress();
        if (factoryAddress == null || factoryAddress.isBlank()) {
            log.error("Indexer enabled but no factory address configured (livana.indexer.factory-address) — indexer will NOT start");
            return;
        }

        factoryAddress = factoryAddress.toLowerCase();
        registerContract(factoryAddress, "POOL_FACTORY");

        // Resolve SBT address — from config or from factory.sbt() on-chain
        String sbtAddress = resolveSbtAddress(factoryAddress);
        if (sbtAddress == null) {
            log.error("Cannot resolve SBT contract address — indexer will NOT start. " +
                    "Set livana.indexer.sbt-address or ensure factory.sbt() is callable.");
            return;
        }
        registerContract(sbtAddress, "SBT");

        // Register all known pool contracts from the database (pools with valid metadata)
        poolRepository.findAll().forEach(pool ->
                registerContract(pool.getOnChainAddress().toLowerCase(), "FUND_POOL"));

        // Also recover pools tracked in previous runs but not in the pools table
        // (e.g., pools with invalid IPFS metadata that were still tracked for donations/events)
        indexerStateRepository.findByContractType("FUND_POOL").forEach(state ->
                registerContract(state.getContractAddress().toLowerCase(), "FUND_POOL"));

        log.info("Event indexer initialized — tracking {} contracts (factory={}, sbt={})",
                trackedContracts.size(), factoryAddress, sbtAddress);
        initialized = true;
    }

    /**
     * Resolve the SBT contract address.
     * Priority: config property → factory.sbt() on-chain call → null (fail).
     */
    private String resolveSbtAddress(String factoryAddress) {
        // 1. Check config
        String configured = properties.getSbtAddress();
        if (configured != null && !configured.isBlank()) {
            log.info("Using configured SBT address: {}", configured);
            return configured.toLowerCase();
        }

        // 2. Call factory.sbt() on-chain
        log.info("SBT address not configured — reading from factory.sbt() on-chain...");
        try {
            Function sbtFn = new Function("sbt", Collections.emptyList(),
                    List.of(new TypeReference<Address>() {}));
            String encoded = FunctionEncoder.encode(sbtFn);
            Transaction tx = Transaction.createEthCallTransaction(null, factoryAddress, encoded);
            String result = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send().getValue();
            List<Type> decoded = FunctionReturnDecoder.decode(result, sbtFn.getOutputParameters());

            if (decoded.isEmpty()) {
                log.error("factory.sbt() returned empty result");
                return null;
            }

            String sbtAddress = ((Address) decoded.get(0)).getValue().toLowerCase();
            if ("0x0000000000000000000000000000000000000000".equals(sbtAddress)) {
                log.error("factory.sbt() returned zero address — SBT not deployed yet?");
                return null;
            }

            log.info("Resolved SBT address from factory.sbt(): {}", sbtAddress);
            return sbtAddress;
        } catch (Exception e) {
            log.error("Failed to call factory.sbt() at {}: {}", factoryAddress, e.getMessage());
            return null;
        }
    }

    /**
     * Main polling loop. Runs at a fixed rate configured by livana.indexer.poll-interval-ms.
     * For each tracked contract, queries eth_getLogs from lastIndexedBlock+1 to latestBlock-confirmations.
     */
    @Scheduled(fixedDelayString = "${livana.indexer.poll-interval-ms:3000}")
    public void poll() {
        if (!initialized || !properties.isEnabled()) {
            return;
        }

        try {
            BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
            long safeBlock = latestBlock.longValue() - properties.getConfirmations();

            if (safeBlock < 0) {
                return;
            }

            // Take a snapshot of currently tracked contracts to avoid CME
            Map<String, String> snapshot = new HashMap<>(trackedContracts);

            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                String contractAddress = entry.getKey();
                String contractType = entry.getValue();

                try {
                    pollContract(contractAddress, contractType, safeBlock);
                } catch (Exception e) {
                    log.error("Error polling contract {} ({}): {}",
                            contractAddress, contractType, e.getMessage());
                }
            }

            // Evict old block timestamps to prevent memory growth
            if (blockTimestampCache.size() > 10_000) {
                long cutoff = safeBlock - 5000;
                blockTimestampCache.entrySet().removeIf(e -> e.getKey() < cutoff);
            }
        } catch (Exception e) {
            log.error("Polling loop error: {}", e.getMessage());
        }
    }

    private void pollContract(String contractAddress, String contractType, long safeBlock) throws Exception {
        long fromBlock = getLastIndexedBlock(contractAddress) + 1;

        if (fromBlock > safeBlock) {
            return; // Already up to date
        }

        long batchSize = properties.getBatchSize();

        // Process in batches to respect RPC provider limits
        while (fromBlock <= safeBlock) {
            long toBlock = Math.min(fromBlock + batchSize - 1, safeBlock);

            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
                    contractAddress
            );

            EthLog ethLog = web3j.ethGetLogs(filter).send();

            if (ethLog.hasError()) {
                log.error("eth_getLogs error for {} [{}-{}]: {}",
                        contractAddress, fromBlock, toBlock, ethLog.getError().getMessage());
                break;
            }

            List<EthLog.LogResult> results = ethLog.getLogs();
            if (results != null && !results.isEmpty()) {
                log.debug("Processing {} logs for {} blocks [{}-{}]",
                        results.size(), contractAddress, fromBlock, toBlock);

                for (EthLog.LogResult<?> result : results) {
                    if (result instanceof EthLog.LogObject logObject) {
                        Log eventLog = logObject.get();
                        OffsetDateTime blockTimestamp = getBlockTimestamp(eventLog.getBlockNumber().longValue());
                        boolean newPool = eventPersistenceService.processLog(eventLog, blockTimestamp);

                        if (newPool && "POOL_FACTORY".equals(contractType)) {
                            // A PoolDeployed event — register the new pool for tracking
                            String newPoolAddress = eventLog.getTopics().size() > 1
                                    ? ("0x" + eventLog.getTopics().get(1).substring(26)).toLowerCase()
                                    : null;
                            if (newPoolAddress != null && !trackedContracts.containsKey(newPoolAddress)) {
                                registerContract(newPoolAddress, "FUND_POOL");
                                log.info("Dynamically tracking new pool: {}", newPoolAddress);
                            }
                        }
                    }
                }
            }

            // Update indexer state
            updateLastIndexedBlock(contractAddress, contractType, toBlock);
            fromBlock = toBlock + 1;
        }
    }

    // ========================================================================
    // State management
    // ========================================================================

    private void registerContract(String address, String contractType) {
        trackedContracts.put(address, contractType);
        log.debug("Registered contract for tracking: {} ({})", address, contractType);
    }

    private long getLastIndexedBlock(String contractAddress) {
        return indexerStateRepository.findByContractAddress(contractAddress)
                .map(IndexerState::getLastIndexedBlock)
                .orElse(0L);
    }

    private void updateLastIndexedBlock(String contractAddress, String contractType, long blockNumber) {
        IndexerState state = indexerStateRepository.findByContractAddress(contractAddress)
                .orElseGet(() -> IndexerState.builder()
                        .contractAddress(contractAddress)
                        .contractType(contractType)
                        .lastIndexedBlock(0L)
                        .build());
        state.setLastIndexedBlock(blockNumber);
        indexerStateRepository.save(state);
    }

    /**
     * Fetch and cache block timestamp.
     * Uses eth_getBlockByNumber (without full transactions) to get the timestamp.
     */
    private OffsetDateTime getBlockTimestamp(long blockNumber) {
        return blockTimestampCache.computeIfAbsent(blockNumber, bn -> {
            try {
                EthBlock ethBlock = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(BigInteger.valueOf(bn)), false).send();
                BigInteger timestamp = ethBlock.getBlock().getTimestamp();
                return OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp.longValue()), ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Failed to fetch timestamp for block {}: {}", bn, e.getMessage());
                return OffsetDateTime.now();
            }
        });
    }
}
