package com.livana.backend.pool.service;

import com.livana.backend.common.exception.ApiException;
import com.livana.backend.donation.dto.PoolDonationDto;
import com.livana.backend.donation.entity.Donation;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.pool.dto.PoolDetailDto;
import com.livana.backend.pool.dto.PoolSummaryDto;
import com.livana.backend.pool.entity.Pool;
import com.livana.backend.pool.repository.PoolRepository;
import com.livana.backend.proof.dto.ProofDto;
import com.livana.backend.proof.entity.Proof;
import com.livana.backend.proof.repository.ProofRepository;
import com.livana.backend.reputation.dto.NgoReputationDto;
import com.livana.backend.reputation.service.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoolQueryService {

    private final PoolRepository poolRepository;
    private final DonationRepository donationRepository;
    private final ProofRepository proofRepository;
    private final ReputationService reputationService;

    public Page<PoolSummaryDto> listPools(String region, String search, Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "deployedAt");

        Pageable capped = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                sort
        );

        boolean hasRegion = region != null && !region.isBlank();
        boolean hasSearch = search != null && !search.trim().isEmpty();

        Page<Pool> page;

        if (hasRegion && hasSearch) {
            page = poolRepository.findByRegionIgnoreCaseAndTitleContaining(region, search.trim(), capped);
        } else if (hasRegion) {
            page = poolRepository.findByRegionIgnoreCase(region, capped);
        } else if (hasSearch) {
            page = poolRepository.findByTitleContainingIgnoreCase(search.trim(), capped);
        } else {
            page = poolRepository.findAllBy(capped);
        }

        return page.map(this::toSummaryDto);
    }

    public PoolDetailDto getPool(String address) {
        Pool pool = poolRepository.findByOnChainAddress(address)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POOL_NOT_FOUND",
                        "Pool not found: " + address
                ));

        return toDetailDto(pool);
    }

    private PoolSummaryDto toSummaryDto(Pool pool) {
        return new PoolSummaryDto(
                pool.getOnChainAddress(),
                pool.getTitle(),
                pool.getDescription(),
                pool.getRegion(),
                pool.getCoverImageCid(),
                pool.getTargetAmount(),
                pool.getTotalDonated(),
                pool.getTotalReleased(),
                pool.getIsPaused(),
                pool.getDeployedAt()
        );
    }

    private PoolDetailDto toDetailDto(Pool pool) {
        String poolAddress = pool.getOnChainAddress();

        // Fetch recent donations (last 10)
        Pageable recentPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "blockTimestamp"));
        Page<Donation> donationsPage = donationRepository.findByPoolAddress(poolAddress, recentPage);
        List<PoolDonationDto> recentDonations = donationsPage.getContent().stream()
                .map(d -> new PoolDonationDto(d.getDonorAddress(), d.getAmount(), d.getTxHash(), d.getBlockTimestamp()))
                .toList();
        long donationCount = donationsPage.getTotalElements();

        // Fetch recent proofs (last 10)
        Pageable proofsPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<Proof> proofsResult = proofRepository.findByPoolAddress(poolAddress, proofsPage);
        List<ProofDto> recentProofs = proofsResult.getContent().stream()
                .map(p -> new ProofDto(p.getProofId(), p.getIpfsCid(), p.getAmount(), p.getReleased(), p.getSubmittedAt(), p.getReleasedAt()))
                .toList();
        long proofCount = proofsResult.getTotalElements();

        // Fetch creator reputation
        NgoReputationDto creatorReputation = reputationService.getReputation(pool.getCreatorAddress());

        return new PoolDetailDto(
                pool.getOnChainAddress(),
                pool.getCreatorAddress(),
                pool.getPoolIndex(),
                pool.getMetadataCid(),
                pool.getTitle(),
                pool.getDescription(),
                pool.getRegion(),
                pool.getCoverImageCid(),
                pool.getTargetAmount(),
                pool.getTotalDonated(),
                pool.getTotalReleased(),
                pool.getIsPaused(),
                pool.getDeployTxHash(),
                pool.getDeployBlock(),
                pool.getDeployedAt(),
                donationCount,
                proofCount,
                recentDonations,
                recentProofs,
                creatorReputation
        );
    }
}
