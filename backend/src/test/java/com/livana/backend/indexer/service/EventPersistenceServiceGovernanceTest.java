package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.repository.IndexedEventRepository;
import com.livana.backend.ngo.service.NgoApplicationService;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
import com.livana.backend.donation.repository.DonationRepository;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPersistenceService — NGO governance and pool pause/unpause paths.
 *
 * Production behaviors protected:
 * - NGOApproved creates an IndexedEvent and calls NgoApplicationService.onNgoApprovedEvent.
 * - NGORevoked creates an IndexedEvent but does NOT call NgoApplicationService.
 * - Paused sets Pool.isPaused = true when the pool exists.
 * - Unpaused sets Pool.isPaused = false when the pool exists.
 * - Paused/Unpaused for unknown pools do not crash (graceful no-op).
 */
@ExtendWith(MockitoExtension.class)
class EventPersistenceServiceGovernanceTest {

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

    private static final String FACTORY_ADDRESS = "0xfactoryaddress00000000000000000000000000";
    private static final String NGO_ADDRESS = "0x2222222222222222222222222222222222222222";
    private static final String POOL_ADDRESS = "0xaabbccddee1122334455667788990011aabbccdd";
    private static final String PAUSER_ADDRESS = "0x9999999999999999999999999999999999999999";
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
    // Tests — NGOApproved
    // ========================================================================

    @Nested
    @DisplayName("NGOApproved handling")
    class NgoApprovedHandling {

        @Test
        @DisplayName("NGOApproved saves an IndexedEvent with correct decoded ngo address in rawData")
        void savesIndexedEvent() {
            Log log = IndexerLogTestFactory.ngoApproved("0xtxngo01", 0, FACTORY_ADDRESS, NGO_ADDRESS);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxngo01", 0)).thenReturn(false);

            service.processLog(log, BLOCK_TIMESTAMP);

            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent saved = indexedEventCaptor.getValue();

            assertThat(saved.getEventType()).isEqualTo("NGO_APPROVED");
            assertThat(saved.getContractAddress()).isEqualTo(FACTORY_ADDRESS.toLowerCase());
            assertThat(saved.getTxHash()).isEqualTo("0xtxngo01");
            assertThat(saved.getLogIndex()).isEqualTo(0);
            assertThat(saved.getBlockTimestamp()).isEqualTo(BLOCK_TIMESTAMP);
            // Verify exact decoded ngo address in rawData
            assertThat(saved.getRawData().get("ngo").asText())
                    .isEqualTo(NGO_ADDRESS.toLowerCase());
        }

        @Test
        @DisplayName("NGOApproved calls NgoApplicationService.onNgoApprovedEvent with decoded address")
        void callsNgoApplicationServiceApproval() {
            Log log = IndexerLogTestFactory.ngoApproved("0xtxngo02", 1, FACTORY_ADDRESS, NGO_ADDRESS);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxngo02", 1)).thenReturn(false);

            service.processLog(log, BLOCK_TIMESTAMP);

            // The decoded address should be lowercase, matching decodeIndexedAddress behavior
            verify(ngoApplicationService).onNgoApprovedEvent(NGO_ADDRESS.toLowerCase());
        }
    }

    // ========================================================================
    // Tests — NGORevoked
    // ========================================================================

    @Nested
    @DisplayName("NGORevoked handling")
    class NgoRevokedHandling {

        @Test
        @DisplayName("NGORevoked saves an IndexedEvent with decoded ngo but does not call NgoApplicationService")
        void savesEventWithoutApprovalSideEffects() {
            Log log = IndexerLogTestFactory.ngoRevoked("0xtxrev01", 0, FACTORY_ADDRESS, NGO_ADDRESS, 2001L);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxrev01", 0)).thenReturn(false);

            service.processLog(log, BLOCK_TIMESTAMP);

            // IndexedEvent must be saved
            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent saved = indexedEventCaptor.getValue();

            assertThat(saved.getEventType()).isEqualTo("NGO_REVOKED");
            assertThat(saved.getContractAddress()).isEqualTo(FACTORY_ADDRESS.toLowerCase());
            assertThat(saved.getTxHash()).isEqualTo("0xtxrev01");
            // Verify exact decoded ngo address in rawData
            assertThat(saved.getRawData().get("ngo").asText())
                    .isEqualTo(NGO_ADDRESS.toLowerCase());

            // Must NOT call any NgoApplicationService methods
            verifyNoInteractions(ngoApplicationService);
        }
    }

    // ========================================================================
    // Tests — Paused
    // ========================================================================

    @Nested
    @DisplayName("Paused handling")
    class PausedHandling {

        @Test
        @DisplayName("Paused saves an IndexedEvent and sets Pool.isPaused true when pool exists")
        void setsPoolPausedTrue() {
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), 0L, 0L, false);

            Log log = IndexerLogTestFactory.paused("0xtxpause01", 0, POOL_ADDRESS, PAUSER_ADDRESS, 3000L);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxpause01", 0)).thenReturn(false);
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            // IndexedEvent saved
            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent savedEvent = indexedEventCaptor.getValue();
            assertThat(savedEvent.getEventType()).isEqualTo("PAUSED");
            assertThat(savedEvent.getContractAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());

            // Pool updated with isPaused = true
            verify(poolRepository).save(poolCaptor.capture());
            Pool savedPool = poolCaptor.getValue();
            assertThat(savedPool.getIsPaused()).isTrue();
        }

        @Test
        @DisplayName("Paused for unknown pool does not crash")
        void unknownPoolDoesNotCrash() {
            Log log = IndexerLogTestFactory.paused("0xtxpause02", 0,
                    "0xunknownpool0000000000000000000000000000", PAUSER_ADDRESS, 3000L);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxpause02", 0)).thenReturn(false);
            when(poolRepository.findByOnChainAddress("0xunknownpool0000000000000000000000000000"))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.processLog(log, BLOCK_TIMESTAMP));

            // IndexedEvent is still saved
            verify(indexedEventRepository).save(any());
            // Pool is NOT saved — it doesn't exist
            verify(poolRepository, never()).save(any());
        }
    }

    // ========================================================================
    // Tests — Unpaused
    // ========================================================================

    @Nested
    @DisplayName("Unpaused handling")
    class UnpausedHandling {

        @Test
        @DisplayName("Unpaused saves an IndexedEvent and sets Pool.isPaused false when pool exists")
        void setsPoolPausedFalse() {
            Pool pool = IndexerLogTestFactory.pool(POOL_ADDRESS.toLowerCase(), 0L, 0L, true);

            Log log = IndexerLogTestFactory.unpaused("0xtxunpause01", 0, POOL_ADDRESS, PAUSER_ADDRESS, 3001L);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxunpause01", 0)).thenReturn(false);
            when(poolRepository.findByOnChainAddress(POOL_ADDRESS.toLowerCase())).thenReturn(Optional.of(pool));

            service.processLog(log, BLOCK_TIMESTAMP);

            // IndexedEvent saved
            verify(indexedEventRepository).save(indexedEventCaptor.capture());
            IndexedEvent savedEvent = indexedEventCaptor.getValue();
            assertThat(savedEvent.getEventType()).isEqualTo("UNPAUSED");
            assertThat(savedEvent.getContractAddress()).isEqualTo(POOL_ADDRESS.toLowerCase());

            // Pool updated with isPaused = false
            verify(poolRepository).save(poolCaptor.capture());
            Pool savedPool = poolCaptor.getValue();
            assertThat(savedPool.getIsPaused()).isFalse();
        }

        @Test
        @DisplayName("Unpaused for unknown pool does not crash")
        void unknownPoolDoesNotCrash() {
            Log log = IndexerLogTestFactory.unpaused("0xtxunpause02", 0,
                    "0xunknownpool0000000000000000000000000000", PAUSER_ADDRESS, 3001L);

            when(indexedEventRepository.existsByTxHashAndLogIndex("0xtxunpause02", 0)).thenReturn(false);
            when(poolRepository.findByOnChainAddress("0xunknownpool0000000000000000000000000000"))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.processLog(log, BLOCK_TIMESTAMP));

            // IndexedEvent is still saved
            verify(indexedEventRepository).save(any());
            // Pool is NOT saved — it doesn't exist
            verify(poolRepository, never()).save(any());
        }
    }
}
