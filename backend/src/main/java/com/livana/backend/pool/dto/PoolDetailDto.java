package com.livana.backend.pool.dto;

import com.livana.backend.donation.dto.PoolDonationDto;
import com.livana.backend.proof.dto.ProofDto;
import com.livana.backend.reputation.dto.NgoReputationDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full pool detail including recent donations, proofs, and creator reputation.
 * PRD: "Detail endpoint joins cached metadata with on-chain indexed data
 * (donations, proofs, releases)."
 */
public record PoolDetailDto(
    String onChainAddress,
    String creatorAddress,
    int poolIndex,
    String metadataCid,
    String title,
    String description,
    String region,
    String coverImageCid,
    long targetAmount,
    long totalDonated,
    long totalReleased,
    boolean isPaused,
    String deployTxHash,
    long deployBlock,
    OffsetDateTime deployedAt,
    long donationCount,
    long proofCount,
    List<PoolDonationDto> recentDonations,
    List<ProofDto> recentProofs,
    NgoReputationDto creatorReputation
) {}
