package com.livana.backend.reputation.service;

import com.livana.backend.common.exception.ApiException;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.entity.NgoApplication;
import com.livana.backend.ngo.repository.NgoApplicationRepository;
import com.livana.backend.reputation.dto.NgoLeaderboardEntryDto;
import com.livana.backend.reputation.dto.NgoReputationDto;
import com.livana.backend.reputation.dto.SbtMintDto;
import com.livana.backend.reputation.entity.SbtMint;
import com.livana.backend.reputation.repository.NgoLeaderboardProjection;
import com.livana.backend.reputation.repository.NgoReputationProjection;
import com.livana.backend.reputation.repository.SbtMintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReputationService {

    private final SbtMintRepository sbtMintRepository;
    private final NgoApplicationRepository ngoApplicationRepository;

    public NgoReputationDto getReputation(String ngoAddress) {
        String orgName = resolveOrgName(ngoAddress);
        NgoReputationProjection projection = sbtMintRepository.findReputationByNgoAddress(ngoAddress);

        if (projection == null || projection.getTotalSbts() == null) {
            return new NgoReputationDto(ngoAddress, orgName, 0, 0, 0);
        }

        return new NgoReputationDto(
                ngoAddress,
                orgName,
                projection.getTotalSbts(),
                projection.getTotalAmountReleased(),
                projection.getPoolCount()
        );
    }

    /**
     * Resolve the public display name for a verified NGO by wallet address.
     * Only approved applications expose a name; returns null for unknown/unverified
     * addresses so the field degrades gracefully (callers fall back to the address).
     */
    private String resolveOrgName(String ngoAddress) {
        return ngoApplicationRepository
                .findByWalletAddressAndStatus(ngoAddress, ApplicationStatus.APPROVED)
                .map(NgoApplication::getOrgName)
                .orElse(null);
    }

    /**
     * Paginated SBT history for an NGO.
     * PRD: "As an NGO operator, I want to see my own SBT history and cumulative reputation."
     */
    public Page<SbtMintDto> getSbtHistory(String ngoAddress, Pageable pageable) {
        Pageable capped = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "blockTimestamp")
        );
        Page<SbtMint> page = sbtMintRepository.findByNgoAddress(ngoAddress, capped);
        return page.map(this::toSbtMintDto);
    }

    public List<NgoLeaderboardEntryDto> leaderboard(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                    "limit must be between 1 and 100");
        }

        List<NgoLeaderboardProjection> projections =
                sbtMintRepository.findNgoLeaderboard(PageRequest.of(0, limit));

        return IntStream.range(0, projections.size())
                .mapToObj(i -> new NgoLeaderboardEntryDto(
                        projections.get(i).getNgoAddress(),
                        projections.get(i).getTotalSbts(),
                        projections.get(i).getTotalAmountReleased(),
                        projections.get(i).getPoolCount(),
                        i + 1))
                .toList();
    }

    private SbtMintDto toSbtMintDto(SbtMint mint) {
        return new SbtMintDto(
                mint.getTokenId(),
                mint.getPoolAddress(),
                mint.getAmount(),
                mint.getTxHash(),
                mint.getBlockTimestamp()
        );
    }
}
