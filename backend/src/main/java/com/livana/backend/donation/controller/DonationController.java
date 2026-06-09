package com.livana.backend.donation.controller;

import com.livana.backend.auth.service.UserService;
import com.livana.backend.common.validation.AddressValidator;
import com.livana.backend.donation.dto.DonorDonationDto;
import com.livana.backend.donation.dto.LeaderboardEntryDto;
import com.livana.backend.donation.dto.PoolDonationDto;
import com.livana.backend.donation.service.DonationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationQueryService donationQueryService;
    private final UserService userService;

    @GetMapping("/pool/{poolAddress}")
    public ResponseEntity<Page<PoolDonationDto>> donationsByPool(
            @PathVariable String poolAddress,
            @PageableDefault(size = 20) Pageable pageable) {
        String normalized = AddressValidator.validateAndNormalize(poolAddress, "poolAddress");
        Page<PoolDonationDto> page = donationQueryService.donationsByPool(normalized, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get donation history for the authenticated user's wallet.
     * PRD: "As a donor, I want to see all MY donations across all pools."
     * Uses /me to make it clear only your own history is accessible.
     */
    @GetMapping("/me")
    public ResponseEntity<Page<DonorDonationDto>> myDonations(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();
        var user = userService.getAuthenticatedUserWithWallet(clerkId);
        Page<DonorDonationDto> page = donationQueryService.donationsByDonor(user.getWalletAddress(), pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        List<LeaderboardEntryDto> entries = donationQueryService.leaderboard(limit);
        return ResponseEntity.ok(entries);
    }
}
