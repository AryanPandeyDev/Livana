package com.livana.backend.reputation.controller;

import com.livana.backend.common.validation.AddressValidator;
import com.livana.backend.reputation.dto.NgoLeaderboardEntryDto;
import com.livana.backend.reputation.dto.NgoReputationDto;
import com.livana.backend.reputation.dto.SbtMintDto;
import com.livana.backend.reputation.service.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reputation")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/{ngoAddress}")
    public ResponseEntity<NgoReputationDto> getReputation(@PathVariable String ngoAddress) {
        String normalized = AddressValidator.validateAndNormalize(ngoAddress, "ngoAddress");
        NgoReputationDto reputation = reputationService.getReputation(normalized);
        return ResponseEntity.ok(reputation);
    }

    /**
     * Paginated SBT history for an NGO.
     * PRD: "As an NGO operator, I want to see my own SBT history and cumulative reputation."
     */
    @GetMapping("/{ngoAddress}/history")
    public ResponseEntity<Page<SbtMintDto>> getSbtHistory(
            @PathVariable String ngoAddress,
            @PageableDefault(size = 20) Pageable pageable) {
        String normalized = AddressValidator.validateAndNormalize(ngoAddress, "ngoAddress");
        Page<SbtMintDto> page = reputationService.getSbtHistory(normalized, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<NgoLeaderboardEntryDto>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        List<NgoLeaderboardEntryDto> entries = reputationService.leaderboard(limit);
        return ResponseEntity.ok(entries);
    }
}
