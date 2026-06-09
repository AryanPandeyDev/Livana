package com.livana.backend.proof.controller;

import com.livana.backend.auth.entity.User;
import com.livana.backend.auth.service.UserService;
import com.livana.backend.common.validation.AddressValidator;
import com.livana.backend.proof.dto.NgoProofDto;
import com.livana.backend.proof.dto.PendingProofDto;
import com.livana.backend.proof.dto.ProofDto;
import com.livana.backend.proof.service.ProofQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProofController {

    private final ProofQueryService proofQueryService;
    private final UserService userService;

    @GetMapping("/api/v1/proofs/pool/{poolAddress}")
    public ResponseEntity<Page<ProofDto>> proofsByPool(
            @PathVariable String poolAddress,
            @PageableDefault(size = 20) Pageable pageable) {
        String normalized = AddressValidator.validateAndNormalize(poolAddress, "poolAddress");
        Page<ProofDto> page = proofQueryService.proofsByPool(normalized, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * NGO view: all proof submissions across every pool the authenticated NGO
     * created, with release status. Serves PRD User Story 27.
     *
     * Self-scoped: the NGO wallet is resolved from the authenticated user's
     * profile, so an NGO can only ever see their own proofs. Requires a linked
     * wallet (403 WALLET_NOT_LINKED otherwise).
     */
    @GetMapping("/api/v1/proofs/me")
    public ResponseEntity<Page<NgoProofDto>> myProofs(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getAuthenticatedUserWithWallet(jwt.getSubject());
        Page<NgoProofDto> page = proofQueryService.proofsByCreator(user.getWalletAddress(), pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/api/v1/admin/proofs/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PendingProofDto>> pendingProofs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PendingProofDto> page = proofQueryService.pendingProofs(pageable);
        return ResponseEntity.ok(page);
    }
}
