package com.livana.backend.indexer.repository;

import com.livana.backend.indexer.entity.IndexerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndexerStateRepository extends JpaRepository<IndexerState, UUID> {

    Optional<IndexerState> findByContractAddress(String contractAddress);

    List<IndexerState> findByContractType(String contractType);
}
