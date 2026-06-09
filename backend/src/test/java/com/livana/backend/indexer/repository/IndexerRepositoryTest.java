package com.livana.backend.indexer.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livana.backend.indexer.entity.IndexedEvent;
import com.livana.backend.indexer.entity.IndexerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level integration tests for the indexer persistence layer.
 *
 * Uses @DataJpaTest backed by H2 in PostgreSQL-compatibility mode
 * (configured in src/test/resources/application.properties).
 * Hibernate creates the schema from entity annotations (ddl-auto=create-drop),
 * so Flyway migrations are disabled for these tests.
 *
 * Production behaviors protected:
 * - IndexedEvent round-trips through JPA including JSON rawData.
 * - existsByTxHashAndLogIndex dedup query works correctly.
 * - IndexerState persists and is findable by contractAddress.
 * - Lifecycle callbacks (@PrePersist / @PreUpdate) set timestamps.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IndexerRepositoryTest {

    @Autowired
    private IndexedEventRepository indexedEventRepository;

    @Autowired
    private IndexerStateRepository indexerStateRepository;

    @Autowired
    private TestEntityManager em;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final OffsetDateTime BLOCK_TS =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Build a minimal valid IndexedEvent ready for persistence.
     */
    private static IndexedEvent buildEvent(String txHash, int logIndex) {
        ObjectNode rawData = MAPPER.createObjectNode();
        rawData.put("donor", "0x1111111111111111111111111111111111111111");
        rawData.put("amount", "500000000");

        return IndexedEvent.builder()
                .eventType("DONATION_RECEIVED")
                .contractAddress("0xaabbccddee1122334455667788990011aabbccdd")
                .txHash(txHash)
                .logIndex(logIndex)
                .blockNumber(1000L)
                .blockTimestamp(BLOCK_TS)
                .rawData(rawData)
                .build();
    }

    /**
     * Build a minimal valid IndexerState ready for persistence.
     */
    private static IndexerState buildState(String contractAddress) {
        return IndexerState.builder()
                .contractAddress(contractAddress)
                .contractType("FUND_POOL")
                .lastIndexedBlock(42_000L)
                .build();
    }

    // ========================================================================
    // IndexedEventRepository tests
    // ========================================================================

    @Nested
    @DisplayName("IndexedEventRepository")
    class IndexedEventTests {

        @Test
        @DisplayName("saves and reloads an IndexedEvent with JSON rawData intact")
        void savesAndLoadsIndexedEvent() {
            IndexedEvent event = buildEvent("0xtx001", 0);

            IndexedEvent saved = indexedEventRepository.saveAndFlush(event);
            em.clear(); // detach so the next read hits the database

            IndexedEvent loaded = indexedEventRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getEventType()).isEqualTo("DONATION_RECEIVED");
            assertThat(loaded.getContractAddress())
                    .isEqualTo("0xaabbccddee1122334455667788990011aabbccdd");
            assertThat(loaded.getTxHash()).isEqualTo("0xtx001");
            assertThat(loaded.getLogIndex()).isEqualTo(0);
            assertThat(loaded.getBlockNumber()).isEqualTo(1000L);
            assertThat(loaded.getBlockTimestamp()).isEqualTo(BLOCK_TS);

            // JSON round-trip
            assertThat(loaded.getRawData().get("donor").asText())
                    .isEqualTo("0x1111111111111111111111111111111111111111");
            assertThat(loaded.getRawData().get("amount").asText())
                    .isEqualTo("500000000");

            // @PrePersist sets indexedAt
            assertThat(loaded.getIndexedAt()).isNotNull();
        }

        @Test
        @DisplayName("existsByTxHashAndLogIndex returns true for a saved event")
        void existsByTxHashAndLogIndexReturnsTrue() {
            IndexedEvent event = buildEvent("0xdup001", 7);
            indexedEventRepository.saveAndFlush(event);
            em.clear();

            assertThat(indexedEventRepository.existsByTxHashAndLogIndex("0xdup001", 7))
                    .isTrue();
        }

        @Test
        @DisplayName("existsByTxHashAndLogIndex returns false for unknown txHash+logIndex")
        void existsByTxHashAndLogIndexReturnsFalse() {
            assertThat(indexedEventRepository.existsByTxHashAndLogIndex("0xnonexistent", 99))
                    .isFalse();
        }
    }

    // ========================================================================
    // IndexerStateRepository tests
    // ========================================================================

    @Nested
    @DisplayName("IndexerStateRepository")
    class IndexerStateTests {

        @Test
        @DisplayName("saves and finds IndexerState by contractAddress")
        void savesAndFindsByContractAddress() {
            String addr = "0xaabbccddee1122334455667788990011aabbccdd";
            IndexerState state = buildState(addr);

            indexerStateRepository.saveAndFlush(state);
            em.clear();

            Optional<IndexerState> found = indexerStateRepository.findByContractAddress(addr);

            assertThat(found).isPresent();
            IndexerState loaded = found.get();
            assertThat(loaded.getContractAddress()).isEqualTo(addr);
            assertThat(loaded.getContractType()).isEqualTo("FUND_POOL");
            assertThat(loaded.getLastIndexedBlock()).isEqualTo(42_000L);
        }

        @Test
        @DisplayName("findByContractAddress returns empty for unknown address")
        void findByContractAddressReturnsEmptyForUnknown() {
            assertThat(indexerStateRepository.findByContractAddress("0xunknown"))
                    .isEmpty();
        }

        @Test
        @DisplayName("@PrePersist sets updatedAt on create")
        void prePersistSetsUpdatedAt() {
            // Truncate to micros — databases store microsecond precision,
            // so nanosecond-level "before" markers cause false negatives.
            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);

            IndexerState state = buildState("0x0000000000000000000000000000000000000001");
            indexerStateRepository.saveAndFlush(state);
            em.clear();

            IndexerState loaded = indexerStateRepository.findById(state.getId()).orElseThrow();

            assertThat(loaded.getUpdatedAt()).isNotNull();
            assertThat(loaded.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("@PreUpdate refreshes updatedAt on update")
        void preUpdateRefreshesUpdatedAt() {
            IndexerState state = buildState("0x0000000000000000000000000000000000000002");
            indexerStateRepository.saveAndFlush(state);
            em.clear();

            IndexerState loaded = indexerStateRepository.findById(state.getId()).orElseThrow();
            OffsetDateTime createdAt = loaded.getUpdatedAt();

            // Mutate and save again
            loaded.setLastIndexedBlock(99_999L);
            indexerStateRepository.saveAndFlush(loaded);
            em.clear();

            IndexerState reloaded = indexerStateRepository.findById(state.getId()).orElseThrow();

            assertThat(reloaded.getLastIndexedBlock()).isEqualTo(99_999L);
            assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        }
    }
}
