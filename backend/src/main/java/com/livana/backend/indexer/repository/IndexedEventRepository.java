package com.livana.backend.indexer.repository;

import com.livana.backend.indexer.entity.IndexedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndexedEventRepository extends JpaRepository<IndexedEvent, UUID> {

    boolean existsByTxHashAndLogIndex(String txHash, Integer logIndex);

    /** Find all indexed POOL_DEPLOYED events so we can recover pool addresses on restart. */
    List<IndexedEvent> findByEventType(String eventType);
}
