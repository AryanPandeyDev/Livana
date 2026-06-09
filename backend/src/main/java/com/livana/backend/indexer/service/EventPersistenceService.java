package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livana.backend.donation.entity.Donation;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.repository.IndexedEventRepository;
import com.livana.backend.ngo.service.NgoApplicationService;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
import com.livana.backend.proof.entity.Proof;
import com.livana.backend.proof.repository.ProofRepository;
import com.livana.backend.reputation.entity.SbtMint;
import com.livana.backend.reputation.repository.SbtMintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processes decoded on-chain event logs and persists them to the database.
 *
 * For each event:
 * 1. Store in indexed_events (raw audit log)
 * 2. Denormalize into the appropriate domain table (pools, donations, proofs, sbt_mints)
 * 3. Trigger side effects (e.g., NGO auto-approval)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPersistenceService {

    private final IndexedEventRepository indexedEventRepository;
    private final PoolRepository poolRepository;
    private final DonationRepository donationRepository;
    private final ProofRepository proofRepository;
    private final SbtMintRepository sbtMintRepository;
    private final IpfsMetadataService ipfsMetadataService;
    private final NgoApplicationService ngoApplicationService;
    private final ObjectMapper objectMapper;
    private final Web3j web3j;

    /** Tracks transient IPFS failure count per CID to avoid infinite retries. */
    private static final int MAX_TRANSIENT_RETRIES = 3;
    private final ConcurrentHashMap<String, AtomicInteger> transientFailureCounts = new ConcurrentHashMap<>();

    /**
     * Process a single log entry. Determines the event type from topic0,
     * stores in indexed_events, and dispatches to the appropriate handler.
     *
     * @param eventLog the raw log from eth_getLogs
     * @param blockTimestamp the block timestamp (fetched separately)
     * @return true if a new FundPool was discovered (caller should track it)
     */
    @Transactional
    public boolean processLog(Log eventLog, OffsetDateTime blockTimestamp) {
        String txHash = eventLog.getTransactionHash();
        int logIndex = eventLog.getLogIndex().intValue();

        // Dedup: skip if already indexed (idempotent on restart)
        if (indexedEventRepository.existsByTxHashAndLogIndex(txHash, logIndex)) {
            return false;
        }

        String topic0 = eventLog.getTopics().get(0);
        String eventType = EventDefinitions.SIGNATURE_TO_EVENT_TYPE.get(topic0);

        if (eventType == null) {
            log.trace("Unknown event topic0={} in tx={} — skipping", topic0, txHash);
            return false;
        }

        String contractAddress = eventLog.getAddress().toLowerCase();

        // Build raw_data JSON from decoded parameters
        JsonNode rawData = buildRawData(eventType, eventLog);

        // 1. Store raw event
        IndexedEvent indexedEvent = IndexedEvent.builder()
                .eventType(eventType)
                .contractAddress(contractAddress)
                .txHash(txHash)
                .logIndex(logIndex)
                .blockNumber(eventLog.getBlockNumber().longValue())
                .blockTimestamp(blockTimestamp)
                .rawData(rawData)
                .build();
        indexedEventRepository.save(indexedEvent);

        // 2. Dispatch to handler
        boolean newPoolDiscovered = false;
        switch (eventType) {
            case "POOL_DEPLOYED" -> newPoolDiscovered = handlePoolDeployed(eventLog, blockTimestamp);
            case "DONATION_RECEIVED" -> handleDonationReceived(eventLog, contractAddress, blockTimestamp);
            case "PROOF_SUBMITTED" -> handleProofSubmitted(eventLog, contractAddress, blockTimestamp);
            case "FUNDS_RELEASED" -> handleFundsReleased(eventLog, contractAddress, blockTimestamp);
            case "SBT_LOCKED" -> handleSbtLocked(eventLog, blockTimestamp);
            case "NGO_APPROVED" -> handleNgoApproved(eventLog);
            case "NGO_REVOKED" -> handleNgoRevoked(eventLog);
            case "PAUSED" -> handlePaused(contractAddress, true);
            case "UNPAUSED" -> handlePaused(contractAddress, false);
            case "MULTI_SIG_ADMIN_SET" -> log.info("MultiSigAdminSet event indexed in tx={}", txHash);
            default -> log.debug("Event type {} stored but no handler — raw log only", eventType);
        }

        return newPoolDiscovered;
    }

    // ========================================================================
    // Event handlers
    // ========================================================================

    private boolean handlePoolDeployed(Log eventLog, OffsetDateTime blockTimestamp) {
        // Indexed: poolAddress (topic1), creator (topic2), poolIndex (topic3)
        // Non-indexed: metadataCid (data)
        String poolAddress = decodeIndexedAddress(eventLog, 1);
        String creatorAddress = decodeIndexedAddress(eventLog, 2);
        BigInteger poolIndex = decodeIndexedUint256(eventLog, 3);

        List<Type> nonIndexed = FunctionReturnDecoder.decode(
                eventLog.getData(), EventDefinitions.POOL_DEPLOYED.getNonIndexedParameters());
        String metadataCid = ((Utf8String) nonIndexed.get(0)).getValue();

        log.info("PoolDeployed: address={}, creator={}, index={}, cid={}",
                poolAddress, creatorAddress, poolIndex, metadataCid);

        // Skip if already cached
        if (poolRepository.existsByOnChainAddress(poolAddress)) {
            return false;
        }

        // Fetch and validate IPFS metadata.
        // Three possible outcomes:
        //   VALID            → create pool row
        //   INVALID          → permanent (bad CID, wrong schema). Commit event, skip pool.
        //   TRANSIENT_FAILURE → temporary (gateway down). Throw to roll back → retry next poll.
        var result = ipfsMetadataService.fetchAndValidate(metadataCid);

        if (result instanceof IpfsMetadataService.MetadataResult.TransientFailure tf) {
            int attempts = transientFailureCounts
                    .computeIfAbsent(metadataCid, k -> new AtomicInteger(0))
                    .incrementAndGet();

            if (attempts >= MAX_TRANSIENT_RETRIES) {
                // Give up after repeated failures — treat as permanently invalid.
                transientFailureCounts.remove(metadataCid);
                log.warn("Pool {} CID={} failed {} transient IPFS attempts — giving up. Last: {}",
                        poolAddress, metadataCid, attempts, tf.reason());
                result = new IpfsMetadataService.MetadataResult.Invalid(
                        "Gave up after " + attempts + " transient failures: " + tf.reason());
            } else {
                // Roll back the entire transaction so the event retries on next poll.
                throw new RuntimeException("Transient IPFS failure (attempt " + attempts + "/" +
                        MAX_TRANSIENT_RETRIES + ") for pool " + poolAddress +
                        " CID=" + metadataCid + ": " + tf.reason());
            }
        }

        if (result instanceof IpfsMetadataService.MetadataResult.Invalid inv) {
            // Permanent — the CID content won't change (IPFS is content-addressed).
            log.warn("Pool {} has invalid metadata CID={} — not indexing. Reason: {}",
                    poolAddress, metadataCid, inv.reason());
            return true; // Still track the contract for other events (donations, etc.)
        }

        if (result instanceof IpfsMetadataService.MetadataResult.Valid v) {
            var metadata = v.metadata();
            Pool pool = Pool.builder()
                    .onChainAddress(poolAddress)
                    .creatorAddress(creatorAddress)
                    .poolIndex(poolIndex.intValue())
                    .metadataCid(metadataCid)
                    .title(metadata.title())
                    .description(metadata.description())
                    .region(metadata.region())
                    .coverImageCid(metadata.coverImage())
                    .targetAmount(metadata.targetAmount())
                    .deployTxHash(eventLog.getTransactionHash())
                    .deployBlock(eventLog.getBlockNumber().longValue())
                    .deployedAt(blockTimestamp)
                    .build();
            poolRepository.save(pool);
            return true; // New pool discovered — caller should track it
        }

        // Unreachable with a sealed interface, but satisfies compiler
        return true;
    }

    private void handleDonationReceived(Log eventLog, String contractAddress, OffsetDateTime blockTimestamp) {
        // Indexed: donor (topic1). Non-indexed: amount (data)
        String donorAddress = decodeIndexedAddress(eventLog, 1);

        List<Type> nonIndexed = FunctionReturnDecoder.decode(
                eventLog.getData(), EventDefinitions.DONATION_RECEIVED.getNonIndexedParameters());
        BigInteger amount = ((Uint256) nonIndexed.get(0)).getValue();

        String txHash = eventLog.getTransactionHash();
        int logIndex = eventLog.getLogIndex().intValue();

        if (donationRepository.existsByTxHashAndLogIndex(txHash, logIndex)) {
            return;
        }

        Donation donation = Donation.builder()
                .poolAddress(contractAddress)
                .donorAddress(donorAddress)
                .amount(amount.longValue())
                .txHash(txHash)
                .logIndex(logIndex)
                .blockNumber(eventLog.getBlockNumber().longValue())
                .blockTimestamp(blockTimestamp)
                .build();
        donationRepository.save(donation);

        // Update pool aggregate
        poolRepository.findByOnChainAddress(contractAddress).ifPresent(pool -> {
            pool.setTotalDonated(pool.getTotalDonated() + amount.longValue());
            poolRepository.save(pool);
        });

        log.debug("Indexed DonationReceived: donor={}, amount={}, pool={}", donorAddress, amount, contractAddress);
    }

    private void handleProofSubmitted(Log eventLog, String contractAddress, OffsetDateTime blockTimestamp) {
        // Indexed: proofId (topic1). Non-indexed: ipfsCid, amount (data)
        BigInteger proofId = decodeIndexedUint256(eventLog, 1);

        List<Type> nonIndexed = FunctionReturnDecoder.decode(
                eventLog.getData(), EventDefinitions.PROOF_SUBMITTED.getNonIndexedParameters());
        String ipfsCid = ((Utf8String) nonIndexed.get(0)).getValue();
        BigInteger amount = ((Uint256) nonIndexed.get(1)).getValue();

        Proof proof = Proof.builder()
                .poolAddress(contractAddress)
                .proofId(proofId.intValue())
                .ipfsCid(ipfsCid)
                .amount(amount.longValue())
                .submittedTxHash(eventLog.getTransactionHash())
                .submittedBlock(eventLog.getBlockNumber().longValue())
                .submittedAt(blockTimestamp)
                .build();
        proofRepository.save(proof);

        log.debug("Indexed ProofSubmitted: proofId={}, amount={}, pool={}", proofId, amount, contractAddress);
    }

    private void handleFundsReleased(Log eventLog, String contractAddress, OffsetDateTime blockTimestamp) {
        // Indexed: proofId (topic1), ngo (topic2). Non-indexed: amount (data)
        BigInteger proofId = decodeIndexedUint256(eventLog, 1);

        // Update the existing proof record
        proofRepository.findByPoolAddressAndProofId(contractAddress, proofId.intValue())
                .ifPresentOrElse(proof -> {
                    proof.setReleased(true);
                    proof.setReleasedTxHash(eventLog.getTransactionHash());
                    proof.setReleasedBlock(eventLog.getBlockNumber().longValue());
                    proof.setReleasedAt(blockTimestamp);
                    proofRepository.save(proof);

                    // Update pool aggregate
                    poolRepository.findByOnChainAddress(contractAddress).ifPresent(pool -> {
                        pool.setTotalReleased(pool.getTotalReleased() + proof.getAmount());
                        poolRepository.save(pool);
                    });

                    log.debug("Indexed FundsReleased: proofId={}, pool={}", proofId, contractAddress);
                }, () -> {
                    log.warn("FundsReleased for unknown proof: proofId={}, pool={}", proofId, contractAddress);
                });
    }

    private void handleSbtLocked(Log eventLog, OffsetDateTime blockTimestamp) {
        // Non-indexed: tokenId (data)
        List<Type> nonIndexed = FunctionReturnDecoder.decode(
                eventLog.getData(), EventDefinitions.LOCKED.getNonIndexedParameters());
        BigInteger tokenId = ((Uint256) nonIndexed.get(0)).getValue();

        if (sbtMintRepository.existsByTokenId(tokenId.longValue())) {
            return;
        }

        // From PRD: "The Locked event only has tokenId — the indexer calls
        // sbt.getReputation(tokenId) and sbt.ownerOf(tokenId) to get the full data."
        //
        // IMPORTANT: If on-chain enrichment fails (RPC down, transient error), we let
        // the exception propagate. This rolls back the entire transaction (including
        // the indexed_events insert), so the event will be retried on the next poll
        // cycle. This prevents permanent data loss in sbt_mints.
        String sbtAddress = eventLog.getAddress();

        // Call ownerOf(tokenId) — ERC721 standard
        String ngoAddress = callOwnerOf(sbtAddress, tokenId);

        // Call getReputation(tokenId) — LivanaSBT custom
        ReputationResult reputation = callGetReputation(sbtAddress, tokenId);

        SbtMint mint = SbtMint.builder()
                .tokenId(tokenId.longValue())
                .ngoAddress(ngoAddress)
                .poolAddress(reputation.poolAddress())
                .amount(reputation.amount())
                .txHash(eventLog.getTransactionHash())
                .blockNumber(eventLog.getBlockNumber().longValue())
                .blockTimestamp(blockTimestamp)
                .build();
        sbtMintRepository.save(mint);

        log.info("Indexed SBT mint: tokenId={}, ngo={}, pool={}, amount={}",
                tokenId, ngoAddress, reputation.poolAddress(), reputation.amount());
    }

    private record ReputationResult(String poolAddress, long amount) {}

    /**
     * Call ownerOf(tokenId) on the SBT contract.
     * Throws RuntimeException on failure — callers must let this propagate
     * to roll back the transaction so the event retries on next poll.
     */
    private String callOwnerOf(String sbtAddress, BigInteger tokenId) {
        try {
            org.web3j.abi.datatypes.Function fn = new org.web3j.abi.datatypes.Function(
                    "ownerOf",
                    List.of(new Uint256(tokenId)),
                    List.of(new TypeReference<Address>() {}));
            String encoded = org.web3j.abi.FunctionEncoder.encode(fn);
            org.web3j.protocol.core.methods.request.Transaction tx =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, sbtAddress, encoded);
            String result = web3j.ethCall(tx, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getValue();
            List<Type> decoded = FunctionReturnDecoder.decode(result, fn.getOutputParameters());
            return ((Address) decoded.get(0)).getValue().toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call ownerOf(" + tokenId + ") on SBT " + sbtAddress, e);
        }
    }

    /**
     * Call getReputation(tokenId) on the SBT contract.
     * Returns (pool, amount). Throws RuntimeException on failure.
     */
    private ReputationResult callGetReputation(String sbtAddress, BigInteger tokenId) {
        try {
            org.web3j.abi.datatypes.Function fn = new org.web3j.abi.datatypes.Function(
                    "getReputation",
                    List.of(new Uint256(tokenId)),
                    List.of(new TypeReference<Address>() {},
                            new TypeReference<Uint256>() {},
                            new TypeReference<Uint256>() {}));
            String encoded = org.web3j.abi.FunctionEncoder.encode(fn);
            org.web3j.protocol.core.methods.request.Transaction tx =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, sbtAddress, encoded);
            String result = web3j.ethCall(tx, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getValue();
            List<Type> decoded = FunctionReturnDecoder.decode(result, fn.getOutputParameters());
            String poolAddress = ((Address) decoded.get(0)).getValue().toLowerCase();
            long amount = ((Uint256) decoded.get(1)).getValue().longValue();
            return new ReputationResult(poolAddress, amount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call getReputation(" + tokenId + ") on SBT " + sbtAddress, e);
        }
    }

    private void handleNgoApproved(Log eventLog) {
        String ngoAddress = decodeIndexedAddress(eventLog, 1);
        log.info("NGOApproved event: ngo={}", ngoAddress);
        ngoApplicationService.onNgoApprovedEvent(ngoAddress);
    }

    private void handleNgoRevoked(Log eventLog) {
        String ngoAddress = decodeIndexedAddress(eventLog, 1);
        log.info("NGORevoked event: ngo={}", ngoAddress);
        // No backend side-effect beyond storing the event. The on-chain state
        // (factory.isVerified) is what matters for fund releases.
    }

    private void handlePaused(String contractAddress, boolean paused) {
        poolRepository.findByOnChainAddress(contractAddress).ifPresent(pool -> {
            pool.setIsPaused(paused);
            poolRepository.save(pool);
            log.info("Pool {} {}", contractAddress, paused ? "paused" : "unpaused");
        });
    }

    // ========================================================================
    // Decoding helpers
    // ========================================================================

    /**
     * Decode an indexed address parameter from log topics.
     * Indexed addresses are stored as 32-byte words in topics (left-padded with zeros).
     */
    private String decodeIndexedAddress(Log eventLog, int topicIndex) {
        String raw = eventLog.getTopics().get(topicIndex);
        // Address is last 40 chars of the 64-char hex (strip 0x prefix + 24 leading zeros)
        return "0x" + raw.substring(raw.length() - 40).toLowerCase();
    }

    /**
     * Decode an indexed uint256 parameter from log topics.
     */
    private BigInteger decodeIndexedUint256(Log eventLog, int topicIndex) {
        String raw = eventLog.getTopics().get(topicIndex);
        return new BigInteger(raw.substring(2), 16);
    }

    /**
     * Build a JsonNode from decoded event parameters for the raw_data column.
     * Returns the ObjectNode directly — no intermediate String serialization needed
     * since IndexedEvent.rawData is typed as JsonNode.
     */
    private JsonNode buildRawData(String eventType, Log eventLog) {
        ObjectNode node = objectMapper.createObjectNode();

        try {
            switch (eventType) {
                case "NGO_APPROVED" -> node.put("ngo", decodeIndexedAddress(eventLog, 1));
                case "NGO_REVOKED" -> node.put("ngo", decodeIndexedAddress(eventLog, 1));
                case "MULTI_SIG_ADMIN_SET" -> node.put("multiSigAdmin", decodeIndexedAddress(eventLog, 1));
                case "POOL_DEPLOYED" -> {
                    node.put("poolAddress", decodeIndexedAddress(eventLog, 1));
                    node.put("creator", decodeIndexedAddress(eventLog, 2));
                    node.put("poolIndex", decodeIndexedUint256(eventLog, 3).toString());
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.POOL_DEPLOYED.getNonIndexedParameters());
                    node.put("metadataCid", ((Utf8String) nonIndexed.get(0)).getValue());
                }
                case "DONATION_RECEIVED" -> {
                    node.put("donor", decodeIndexedAddress(eventLog, 1));
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.DONATION_RECEIVED.getNonIndexedParameters());
                    node.put("amount", ((Uint256) nonIndexed.get(0)).getValue().toString());
                }
                case "PROOF_SUBMITTED" -> {
                    node.put("proofId", decodeIndexedUint256(eventLog, 1).toString());
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.PROOF_SUBMITTED.getNonIndexedParameters());
                    node.put("ipfsCid", ((Utf8String) nonIndexed.get(0)).getValue());
                    node.put("amount", ((Uint256) nonIndexed.get(1)).getValue().toString());
                }
                case "FUNDS_RELEASED" -> {
                    node.put("proofId", decodeIndexedUint256(eventLog, 1).toString());
                    node.put("ngo", decodeIndexedAddress(eventLog, 2));
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.FUNDS_RELEASED.getNonIndexedParameters());
                    node.put("amount", ((Uint256) nonIndexed.get(0)).getValue().toString());
                }
                case "SBT_LOCKED" -> {
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.LOCKED.getNonIndexedParameters());
                    node.put("tokenId", ((Uint256) nonIndexed.get(0)).getValue().toString());
                }
                case "PAUSED", "UNPAUSED" -> {
                    List<Type> nonIndexed = FunctionReturnDecoder.decode(
                            eventLog.getData(), EventDefinitions.PAUSED.getNonIndexedParameters());
                    node.put("account", ((Address) nonIndexed.get(0)).getValue());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to decode event params for {}: {}", eventType, e.getMessage());
        }

        return node;
    }
}
