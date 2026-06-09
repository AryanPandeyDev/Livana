package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.repository.IndexedEventRepository;
import com.livana.backend.ngo.service.NgoApplicationService;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
import com.livana.backend.proof.entity.Proof;
import com.livana.backend.proof.repository.ProofRepository;
import com.livana.backend.reputation.repository.SbtMintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPersistenceService — proof and funds release handling only.
 *
 * Production behaviors protected:
 * - ProofSubmitted creates an IndexedEvent (audit) and a Proof row with correct fields.
 * - FundsReleased creates an IndexedEvent, marks existing Proof as released, sets
 *   releasedTxHash/releasedBlock/releasedAt, and updates Pool.totalReleased.
 * - FundsReleased for an unknown proof does not crash and does not update Pool.
 */
@ExtendWith(MockitoExtension.class)
class EventPersistenceServiceProofTest {

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
    @Captor private ArgumentCaptor<Proof> proofCaptor;
    @Captor private ArgumentCaptor<Pool> poolCaptor;

    private EventPersistenceService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String POOL_ADDRESS = "0xaabbccddee1122334455667788990011aabbccdd";
    private static final String NGO_ADDRESS = "0x2222222222222222222222222222222222222222";
    private static final BigInteger PROOF_ID = BigInteger.valueOf(42);
    private static final String IPFS_CID = "QmTestProofCid123456789abcdef";
    private static final BigInteger PROOF_AMOUNT = BigInteger.valueOf(750_000_000L); // 750 USDC (6 dec)
    private static final long BLOCK_NUMBER = 2000L;
    private static final OffsetDateTime BLOCK_TIMESTAMP =
            OffsetDateTime.of(2025, 7, 10, 14, 30, 0, 0, ZoneOffset.UTC);

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
    // Helpers (entity builders only — log builders in IndexerLogTestFactory)
    // ========================================================================

    /**
     * Build a Proof entity representing an already-submitted proof.
     */
    private static Proof buildExistingProof(String poolAddress, int proofId, long amount) {
        return Proof.builder()
                .poolAddress(poolAddress)
                .proofId(proofId)
                .ipfsCid("QmExistingCid")
                .amount(amount)
                .released(false)
                .submittedTxHash("0x" + "bb".repeat(32))
                .submittedBlock(1500L)
                .submittedAt(OffsetDateTime.of(2025, 7, 8, 10, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    // ========================================================================
    // Tests — ProofSubmitted
    // ========================================================================

    @Nested
    @DisplayName("ProofSubmitted handling")
    class ProofSubmittedHandling {

        @Test
        @DisplayName("ProofSubmitted saves an IndexedEvent with correct decoded rawData")
        void savesIndexedEvent() {
            Log log = IndexerLogTestFactory.proofSubmitted(
                    "0xtxproof01", 2, POOL_ADDRESS, PROOF_ID, IPFS_CID, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxproof01", 2))
                    .thenReturn(false);

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent saved = indexedEventCaptor.getValue();

            assertThat(saved.getEventType()).isEqualTo("PROOF_SUBMITTED");
            assertThat(saved.getContractAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
            assertThat(saved.getTxHash()).isEqualTo("0xtxproof01");
            assertThat(saved.getLogIndex()).isEqualTo(2);
            assertThat(saved.getBlockNumber()).isEqualTo(BLOCK_NUMBER);
            assertThat(saved.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
            // Verify exact decoded values in rawData
            assertThat(saved.getRawData().get("proofId").asText())
                    .isEqualTo(PROOF_ID.toString());
            assertThat(saved.getRawData().get("ipfsCid").asText())
                    .isEqualTo(IPFS_CID);
            assertThat(saved.getRawData().get("amount").asText())
                    .isEqualTo(PROOF_AMOUNT.toString());
        }

        @Test
        @DisplayName("ProofSubmitted saves a Proof with correct poolAddress, proofId, ipfsCid, amount, tx hash, block, and timestamp")
        void savesProofWithCorrectFields() {
            Log log = IndexerLogTestFactory.proofSubmitted(
                    "0xtxproof02", 0, POOL_ADDRESS, PROOF_ID, IPFS_CID, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxproof02", 0))
                    .thenReturn(false);

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(proofRepository).save(proofCaptor.capture());
            Proof proof = proofCaptor.getValue();

            assertThat(proof.getPoolAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
            assertThat(proof.getProofId()).isEqualTo(PROOF_ID.intValue());
            assertThat(proof.getIpfsCid()).isEqualTo(IPFS_CID);
            assertThat(proof.getAmount()).isEqualTo(PROOF_AMOUNT.longValue());
            assertThat(proof.getSubmittedTxHash()).isEqualTo("0xtxproof02");
            assertThat(proof.getSubmittedBlock()).isEqualTo(BLOCK_NUMBER);
            assertThat(proof.getSubmittedAt()).isEqualTo(BLOCK_TIMESTAMP);
        }
    }

    // ========================================================================
    // Tests — FundsReleased
    // ========================================================================

    @Nested
    @DisplayName("FundsReleased handling")
    class FundsReleasedHandling {

        @Test
        @DisplayName("FundsReleased saves an IndexedEvent with correct decoded rawData")
        void savesIndexedEvent() {
            Log log = IndexerLogTestFactory.fundsReleased(
                    "0xtxrel01", 1, POOL_ADDRESS, PROOF_ID, NGO_ADDRESS, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrel01", 1))
                    .thenReturn(false);
            // Proof not found — but we still expect IndexedEvent to be saved
            when(proofRepository.findByPoolAddressAndProofId(POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue()))
                    .thenReturn(Optional.empty());

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent saved = indexedEventCaptor.getValue();

            assertThat(saved.getEventType()).isEqualTo("FUNDS_RELEASED");
            assertThat(saved.getContractAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
            assertThat(saved.getTxHash()).isEqualTo("0xtxrel01");
            assertThat(saved.getLogIndex()).isEqualTo(1);
            assertThat(saved.getBlockNumber()).isEqualTo(BLOCK_NUMBER);
            assertThat(saved.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
            // Verify exact decoded values in rawData
            assertThat(saved.getRawData().get("proofId").asText())
                    .isEqualTo(PROOF_ID.toString());
            assertThat(saved.getRawData().get("ngo").asText())
                    .isEqualTo(NGO_ADDRESS.toLowerCase());
            assertThat(saved.getRawData().get("amount").asText())
                    .isEqualTo(PROOF_AMOUNT.toString());
        }

        @Test
        @DisplayName("FundsReleased marks existing Proof as released")
        void marksProofAsReleased() {
            Proof existingProof = buildExistingProof(
                    POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue(), PROOF_AMOUNT.longValue());
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), 0L, 0L, false);

            Log log = IndexerLogTestFactory.fundsReleased(
                    "0xtxrel02", 0, POOL_ADDRESS, PROOF_ID, NGO_ADDRESS, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrel02", 0))
                    .thenReturn(false);
            when(proofRepository.findByPoolAddressAndProofId(POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue()))
                    .thenReturn(Optional.of(existingProof));
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase()))
                    .thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(proofRepository).save(proofCaptor.capture());
            Proof saved = proofCaptor.getValue();

            assertThat(saved.getReleased()).isTrue();
        }

        @Test
        @DisplayName("FundsReleased sets releasedTxHash, releasedBlock, and releasedAt")
        void setsReleasedFields() {
            Proof existingProof = buildExistingProof(
                    POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue(), PROOF_AMOUNT.longValue());
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), 0L, 0L, false);

            Log log = IndexerLogTestFactory.fundsReleased(
                    "0xtxrel03", 4, POOL_ADDRESS, PROOF_ID, NGO_ADDRESS, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrel03", 4))
                    .thenReturn(false);
            when(proofRepository.findByPoolAddressAndProofId(POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue()))
                    .thenReturn(Optional.of(existingProof));
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase()))
                    .thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(proofRepository).save(proofCaptor.capture());
            Proof saved = proofCaptor.getValue();

            assertThat(saved.getReleasedTxHash()).isEqualTo("0xtxrel03");
            assertThat(saved.getReleasedBlock()).isEqualTo(BLOCK_NUMBER);
            assertThat(saved.getReleasedAt()).isEqualTo(BLOCK_TIMESTAMP);
        }

        @Test
        @DisplayName("FundsReleased updates Pool.totalReleased using the proof amount")
        void updatesPoolTotalReleased() {
            long existingReleased = 100_000_000L;
            Proof existingProof = buildExistingProof(
                    POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue(), PROOF_AMOUNT.longValue());
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), 0L, existingReleased, false);

            Log log = IndexerLogTestFactory.fundsReleased(
                    "0xtxrel04", 0, POOL_ADDRESS, PROOF_ID, NGO_ADDRESS, PROOF_AMOUNT, BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrel04", 0))
                    .thenReturn(false);
            when(proofRepository.findByPoolAddressAndProofId(POOL_ADDRESS.toLowerCase(), PROOF_ID.intValue()))
                    .thenReturn(Optional.of(existingProof));
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase()))
                    .thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(poolRepository).save(poolCaptor.capture());
            Pool saved = poolCaptor.getValue();

            assertThat(saved.getTotalReleased())
                    .isEqualTo(existingReleased + PROOF_AMOUNT.longValue());
        }

        @Test
        @DisplayName("FundsReleased for unknown proof does not crash and does not update Pool")
        void unknownProofDoesNotCrashOrUpdatePool() {
            Log log = IndexerLogTestFactory.fundsReleased(
                    "0xtxrel05", 0, POOL_ADDRESS, BigInteger.valueOf(999), NGO_ADDRESS, PROOF_AMOUNT,
                    BLOCK_NUMBER);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrel05", 0))
                    .thenReturn(false);
            when(proofRepository.findByPoolAddressAndProofId(POOL_ADDRESS.toLowerCase(), 999))
                    .thenReturn(Optional.empty());

            // Should not throw
            boolean result = service.processLog(log, BLOCK_TIMESTAMP);

            assertThat(result).isFalse();
            // IndexedEvent is saved (the audit row)
            verify(indexedEventRepository).save(any());
            // Proof is NOT saved — nothing to update
            verify(proofRepository, never()).save(any());
            // Pool is NOT updated — no proof to take the amount from
            verify(poolRepository, never()).save(any());
        }
    }
}
