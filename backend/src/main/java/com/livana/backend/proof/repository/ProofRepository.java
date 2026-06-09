package com.livana.backend.proof.repository;

import com.livana.backend.proof.entity.Proof;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProofRepository extends JpaRepository<Proof, UUID> {

    Page<Proof> findByPoolAddress(String poolAddress, Pageable pageable);

    /**
     * Admin view: pending proofs across all pools.
     */
    Page<Proof> findByReleasedFalse(Pageable pageable);

    /**
     * Match a FundsReleased event to its proof submission.
     */
    Optional<Proof> findByPoolAddressAndProofId(String poolAddress, Integer proofId);

    /**
     * NGO view: all proofs across every pool created by the given NGO wallet.
     *
     * There is no JPA relationship between Proof and Pool (addresses are VARCHAR
     * matches, not FK), so this joins by address: select proofs whose pool_address
     * belongs to a pool whose creator_address is the NGO's wallet.
     */
    @Query("""
        SELECT pr FROM Proof pr
        WHERE pr.poolAddress IN (
            SELECT p.onChainAddress FROM Pool p WHERE p.creatorAddress = :creatorAddress
        )
        """)
    Page<Proof> findByCreatorAddress(@Param("creatorAddress") String creatorAddress, Pageable pageable);
}
