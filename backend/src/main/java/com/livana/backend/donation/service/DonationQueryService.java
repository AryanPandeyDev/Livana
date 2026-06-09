package com.livana.backend.donation.service;

import com.livana.backend.common.exception.ApiException;
import com.livana.backend.donation.dto.DonorDonationDto;
import com.livana.backend.donation.dto.LeaderboardEntryDto;
import com.livana.backend.donation.dto.PoolDonationDto;
import com.livana.backend.donation.entity.Donation;
import com.livana.backend.donation.repository.DonationRepository;
import com.livana.backend.donation.repository.LeaderboardProjection;
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
public class DonationQueryService {

    private final DonationRepository donationRepository;

    public Page<PoolDonationDto> donationsByPool(String poolAddress, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        Page<Donation> page = donationRepository.findByPoolAddress(poolAddress, capped);
        return page.map(this::toPoolDonationDto);
    }

    public Page<DonorDonationDto> donationsByDonor(String donorAddress, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        Page<Donation> page = donationRepository.findByDonorAddress(donorAddress, capped);
        return page.map(this::toDonorDonationDto);
    }

    public List<LeaderboardEntryDto> leaderboard(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PARAMETER",
                    "limit must be between 1 and 100"
            );
        }

        List<LeaderboardProjection> projections =
                donationRepository.findDonorLeaderboard(PageRequest.of(0, limit));

        return projections.stream()
                .map(p -> new LeaderboardEntryDto(
                        p.getDonorAddress(),
                        p.getTotalDonated(),
                        p.getDonationCount()
                ))
                .toList();
    }

    private Pageable capPageable(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "blockTimestamp");

        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                sort
        );
    }

    private PoolDonationDto toPoolDonationDto(Donation donation) {
        return new PoolDonationDto(
                donation.getDonorAddress(),
                donation.getAmount(),
                donation.getTxHash(),
                donation.getBlockTimestamp()
        );
    }

    private DonorDonationDto toDonorDonationDto(Donation donation) {
        return new DonorDonationDto(
                donation.getPoolAddress(),
                donation.getAmount(),
                donation.getTxHash(),
                donation.getBlockTimestamp()
        );
    }
}
