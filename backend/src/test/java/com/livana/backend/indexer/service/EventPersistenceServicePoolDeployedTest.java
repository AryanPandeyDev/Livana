package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.repository.IndexedEventRepository;
import com.livana.backend.indexer.service.IpfsMetadataService.MetadataResult;
import com.livana.backend.indexer.service.IpfsMetadataService.PoolMetadata;
import com.livana.backend.ngo.service.NgoApplicationService;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
import com.livana.backend.proof.repository.ProofRepository;
import com.livana.backend.reputation.repository.SbtMintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPersistenceService — PoolDeployed handling only.
 *
 * Production behaviors protected:
 * - PoolDeployed with valid IPFS metadata saves an IndexedEvent (audit trail).
 * - PoolDeployed with valid IPFS metadata creates a Pool with correct fields.
 * - PoolDeployed returns true so BlockchainPollingService can track the new pool.
 * - PoolDeployed for an already-cached pool does not create a duplicate Pool row.
 * - PoolDeployed with invalid metadata saves IndexedEvent but does not create Pool.
 * - PoolDeployed with transient IPFS failure throws so the caller can retry.
 *
 * NOTE: The transient-failure test proves processLog() throws a RuntimeException.
 * It does NOT prove @Transactional rollback because this is a unit test without
 * Spring's proxy. A Spring integration test with a real DB is needed to prove
 * that the IndexedEvent row is actually rolled back on transient failure.
 */
@ExtendWith(MockitoExtension.class)
class EventPersistenceServicePoolDeployedTest {

    // ========================================================================
    // Mocks
    // ========================================================================
    @Mock private IndexedEventRepository indexedEventRepository;
    @Mock private PoolRepository poolRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private ProofRepository proofRepository;
    @Mock private SbtMintRepository sbtMintRepository;
    @Mock private IpfsMetadataService ipfsMetadataService;
    @Mock private NgoApplicationService ngoApplicationService;
    @Mock private Web3j web3j;

    @Captor private ArgumentCaptor<IndexedEvent> indexedEventCaptor;
    @Captor private ArgumentCaptor<Pool> poolCaptor;

    private EventPersistenceService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test constants
    private static final String POOL_ADDRESS = "0xaabbccddee1122334455667788990011aabbccdd";
    private static final String CREATOR_ADDRESS = "0x2222222222222222222222222222222222222222";
    private static final BigInteger POOL_INDEX = BigInteger.valueOf(7);
    private static final String METADATA_CID = "QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG";
    private static final long BLOCK_NUM = 42_000L;
    private static final OffsetDateTime BLOCK_TIMESTAMP =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final PoolMetadata VALID_METADATA = new PoolMetadata(
            "Clean Water Initiative",
            "Providing clean drinking water to rural communities",
            "East Africa",
            "QmCoverImageCidExample123",
            1_000_000_000L // 1000 USDC
    );

    @BeforeEach
    void setUp() {
        service = new EventPersistenceService(
                indexedEventRepository,
                poolRepository,
                donationRepository,
                proofRepository,
                sbtMintRepository,
                ipfsMetadataService,
                ngoApplicationService,
                objectMapper,
                web3j
        );
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    @DisplayName("PoolDeployed with valid IPFS metadata saves an IndexedEvent with correct decoded rawData")
    void validMetadataSavesIndexedEvent() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_01", 0, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_01", 0)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(false);
        when(ipfsMetadataService.fetchAndValidate(METADATA_CID))
                .thenReturn(new MetadataResult.Valid(VALID_METADATA));

        service.processLog(log, BLOCK_TIMESTAMP);

        verify(indexedEventRepository).save(indexedEventCaptor.capture());
        IndexedEvent saved = indexedEventCaptor.getValue();

        assertThat(saved.getEventType()).isEqualTo("POOL_DEPLOYED");
        assertThat(saved.getTxHash()).isEqualTo("0xtx_pool_01");
        assertThat(saved.getLogIndex()).isEqualTo(0);
        assertThat(saved.getBlockNumber()).isEqualTo(BLOCK_NUM);
        assertThat(saved.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
        // Verify exact decoded values in rawData
        assertThat(saved.getRawData().get("poolAddress").asText())
                .isEqualTo(POOL_ADDRESS.toLowerCase());
        assertThat(saved.getRawData().get("creator").asText())
                .isEqualTo(CREATOR_ADDRESS.toLowerCase());
        assertThat(saved.getRawData().get("poolIndex").asText())
                .isEqualTo(POOL_INDEX.toString());
        assertThat(saved.getRawData().get("metadataCid").asText())
                .isEqualTo(METADATA_CID);
    }

    @Test
    @DisplayName("PoolDeployed with valid IPFS metadata creates a Pool with correct fields from IPFS")
    void validMetadataCreatesPool() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_02", 1, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_02", 1)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(false);
        when(ipfsMetadataService.fetchAndValidate(METADATA_CID))
                .thenReturn(new MetadataResult.Valid(VALID_METADATA));

        service.processLog(log, BLOCK_TIMESTAMP);

        verify(poolRepository).save(poolCaptor.capture());
        Pool pool = poolCaptor.getValue();

        // On-chain fields
        assertThat(pool.getOnChainAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
        assertThat(pool.getCreatorAddress()).isEqualTo(CREATOR_ADDRESS.toLowerCase());
        assertThat(pool.getPoolIndex()).isEqualTo(POOL_INDEX.intValue());
        assertThat(pool.getMetadataCid()).isEqualTo(METADATA_CID);
        assertThat(pool.getDeployTxHash()).isEqualTo("0xtx_pool_02");
        assertThat(pool.getDeployBlock()).isEqualTo(BLOCK_NUM);
        assertThat(pool.getDeployedAt()).isEqualTo(BLOCK_TIMESTAMP);

        // IPFS metadata fields
        assertThat(pool.getTitle()).isEqualTo(VALID_METADATA.title());
        assertThat(pool.getDescription()).isEqualTo(VALID_METADATA.description());
        assertThat(pool.getRegion()).isEqualTo(VALID_METADATA.region());
        assertThat(pool.getCoverImageCid()).isEqualTo(VALID_METADATA.coverImage());
        assertThat(pool.getTargetAmount()).isEqualTo(VALID_METADATA.targetAmount());
    }

    @Test
    @DisplayName("PoolDeployed returns true so BlockchainPollingService can track the new contract")
    void validMetadataReturnsTrue() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_03", 0, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_03", 0)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(false);
        when(ipfsMetadataService.fetchAndValidate(METADATA_CID))
                .thenReturn(new MetadataResult.Valid(VALID_METADATA));

        boolean result = service.processLog(log, BLOCK_TIMESTAMP);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("PoolDeployed for an already-cached pool does not create a duplicate Pool row")
    void existingPoolDoesNotCreateDuplicate() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_04", 0, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_04", 0)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(true);

        boolean result = service.processLog(log, BLOCK_TIMESTAMP);

        // IndexedEvent is still saved (audit trail)
        verify(indexedEventRepository).save(any());
        // But Pool is NOT created — already cached
        verify(poolRepository, never()).save(any());
        // IPFS is not even fetched — short-circuit
        verify(ipfsMetadataService, never()).fetchAndValidate(any());
        // Returns false because it's not a "new" pool discovery
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("PoolDeployed with invalid metadata saves IndexedEvent but does not create Pool")
    void invalidMetadataSavesEventButNoPool() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_05", 2, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_05", 2)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(false);
        when(ipfsMetadataService.fetchAndValidate(METADATA_CID))
                .thenReturn(new MetadataResult.Invalid("Missing required fields"));

        boolean result = service.processLog(log, BLOCK_TIMESTAMP);

        // IndexedEvent is saved — the event happened on-chain regardless
        verify(indexedEventRepository).save(any());
        // Pool is NOT created — metadata is permanently invalid
        verify(poolRepository, never()).save(any());
        // Still returns true — contract should be tracked for future events (donations etc.)
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("PoolDeployed with transient IPFS failure throws RuntimeException so caller can retry")
    void transientIpfsFailureThrowsForRetry() {
        Log log = IndexerLogTestFactory.poolDeployed(
                "0xtx_pool_06", 0, POOL_ADDRESS, CREATOR_ADDRESS, POOL_INDEX, METADATA_CID, BLOCK_NUM);

        when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx_pool_06", 0)).thenReturn(false);
        when(poolRepository.existsByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(false);
        when(ipfsMetadataService.fetchAndValidate(METADATA_CID))
                .thenReturn(new MetadataResult.TransientFailure("IPFS gateway returned HTTP 503"));

        // This unit test proves processLog() throws RuntimeException on transient IPFS failure.
        // In production, Spring's @Transactional proxy catches this and rolls back the
        // transaction (including the IndexedEvent insert), so the event retries on the
        // next poll. Rollback itself is NOT tested here — that requires a Spring integration
        // test with a real database.
        assertThatThrownBy(() -> service.processLog(log, BLOCK_TIMESTAMP))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transient IPFS failure")
                .hasMessageContaining(POOL_ADDRESS.toLowerCase());

        // Pool is definitely not created
        verify(poolRepository, never()).save(any());
    }
}
