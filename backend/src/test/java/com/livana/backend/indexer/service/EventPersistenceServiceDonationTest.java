package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.donation.entity.Donation;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.repository.IndexedEventRepository;
import com.livana.backend.ngo.service.NgoApplicationService;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
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
 * Unit tests for EventPersistenceService — donation handling path only.
 *
 * Production behaviors protected:
 * - Unknown event topics are silently skipped (no IndexedEvent created).
 * - Duplicate txHash+logIndex at the IndexedEvent level is skipped (idempotent).
 * - DonationReceived creates both an IndexedEvent (audit) and a Donation row.
 * - DonationReceived updates Pool.totalDonated aggregate.
 * - Duplicate DonationReceived does not double-count (donation-level dedup).
 */
@ExtendWith(MockitoExtension.class)
class EventPersistenceServiceDonationTest {

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
    @Captor private ArgumentCaptor<Donation> donationCaptor;
    @Captor private ArgumentCaptor<Pool> poolCaptor;

    private EventPersistenceService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String POOL_ADDRESS = "0xaabbccddee1122334455667788990011aabbccdd";
    private static final String DONOR_ADDRESS = "0x1111111111111111111111111111111111111111";
    private static final BigInteger DONATION_AMOUNT = BigInteger.valueOf(500_000_000L); // 500 USDC (6 dec)
    private static final OffsetDateTime BLOCK_TIMESTAMP =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

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

    @Nested
    @DisplayName("Unknown event topics")
    class UnknownEventTopic {

        @Test
        @DisplayName("processLog returns false and does not save anything for an unknown topic0")
        void unknownEventTopicIsSkipped() {
            Log log = IndexerLogTestFactory.unknownEvent("0xabc123", 0);
            when(indexedEventRepository.existsByTxHashAndLogIndex("0xabc123", 0)).thenReturn(false);

            boolean result = service.processLog(log, BLOCK_TIMESTAMP);

            assertThat(result).isFalse();
            verify(indexedEventRepository, never()).save(any());
            verify(donationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Duplicate event dedup (IndexedEvent level)")
    class IndexedEventDedup {

        @Test
        @DisplayName("processLog returns false and skips everything when txHash+logIndex already exists")
        void duplicateTxHashLogIndexIsSkipped() {
            Log log = IndexerLogTestFactory.donationReceived(
                    "0xdup111", 0, POOL_ADDRESS, DONOR_ADDRESS, DONATION_AMOUNT);
            when(indexedEventRepository.existsByTxHashAndLogIndex("0xdup111", 0)).thenReturn(true);

            boolean result = service.processLog(log, BLOCK_TIMESTAMP);

            assertThat(result).isFalse();
            verify(indexedEventRepository, never()).save(any());
            verify(donationRepository, never()).save(any());
            verify(poolRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DonationReceived handling")
    class DonationReceivedHandling {

        @Test
        @DisplayName("DonationReceived saves an IndexedEvent with correct fields and decoded rawData")
        void savesIndexedEvent() {
            Log log = IndexerLogTestFactory.donationReceived(
                    "0xtx001", 3, POOL_ADDRESS, DONOR_ADDRESS, DONATION_AMOUNT);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx001", 3)).thenReturn(false);
            when(donationRepository.existsByTxHashAndLogIndex("0xtx001", 3)).thenReturn(false);
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(Optional.empty());

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent saved = indexedEventCaptor.getValue();

            assertThat(saved.getEventType()).isEqualTo("DONATION_RECEIVED");
            assertThat(saved.getContractAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
            assertThat(saved.getTxHash()).isEqualTo("0xtx001");
            assertThat(saved.getLogIndex()).isEqualTo(3);
            assertThat(saved.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
            // Verify exact decoded values in rawData, not just field existence
            assertThat(saved.getRawData().get("donor").asText())
                    .isEqualTo(DONOR_ADDRESS.toLowerCase());
            assertThat(saved.getRawData().get("amount").asText())
                    .isEqualTo(DONATION_AMOUNT.toString());
        }

        @Test
        @DisplayName("DonationReceived saves a Donation with correct fields")
        void savesDonation() {
            Log log = IndexerLogTestFactory.donationReceived(
                    "0xtx002", 1, POOL_ADDRESS, DONOR_ADDRESS, DONATION_AMOUNT);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx002", 1)).thenReturn(false);
            when(donationRepository.existsByTxHashAndLogIndex("0xtx002", 1)).thenReturn(false);
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(Optional.empty());

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(donationRepository).save(donationCaptor.capture());
            Donation donation = donationCaptor.getValue();

            assertThat(donation.getPoolAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());
            assertThat(donation.getDonorAddress()).isEqualTo(DONOR_ADDRESS.toLowerCase());
            assertThat(donation.getAmount()).isEqualTo(DONATION_AMOUNT.longValue());
            assertThat(donation.getTxHash()).isEqualTo("0xtx002");
            assertThat(donation.getLogIndex()).isEqualTo(1);
            assertThat(donation.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
        }

        @Test
        @DisplayName("DonationReceived updates Pool.totalDonated when pool exists")
        void updatesPoolTotalDonated() {
            long existingDonated = 200_000_000L;
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), existingDonated, 0L, false);

            Log log = IndexerLogTestFactory.donationReceived(
                    "0xtx003", 0, POOL_ADDRESS, DONOR_ADDRESS, DONATION_AMOUNT);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx003", 0)).thenReturn(false);
            when(donationRepository.existsByTxHashAndLogIndex("0xtx003", 0)).thenReturn(false);
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(poolRepository).save(poolCaptor.capture());
            Pool saved = poolCaptor.getValue();

            assertThat(saved.getTotalDonated())
                    .isEqualTo(existingDonated + DONATION_AMOUNT.longValue());
        }

        @Test
        @DisplayName("duplicate DonationReceived (same txHash+logIndex) does not double-count")
        void duplicateDonationReceivedDoesNotDoubleCount() {
            Log log = IndexerLogTestFactory.donationReceived(
                    "0xtx004", 5, POOL_ADDRESS, DONOR_ADDRESS, DONATION_AMOUNT);

            // First call: event-level dedup says not a duplicate
            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtx004", 5)).thenReturn(false);
            // But donation-level dedup says it already exists
            when(donationRepository.existsByTxHashAndLogIndex("0xtx004", 5)).thenReturn(true);

            service.processLog(log, BLOCK_TIMESTAMP);

            // IndexedEvent is saved (it's the first time at the event level)
            verify(indexedEventRepository).save(any());
            // But Donation is NOT saved — the donation-level dedup caught it
            verify(donationRepository, never()).save(any());
            // Pool is NOT updated — no donation was counted
            verify(poolRepository, never()).save(any());
        }
    }
}
