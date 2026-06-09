package com.livana.backend.ngo.controller;

import com.livana.backend.ngo.dto.ApplicationResponse;
import com.livana.backend.ngo.dto.CreateApplicationRequest;
import com.livana.backend.ngo.service.NgoApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * NGO-facing endpoints for managing their own application.
 * All endpoints require authentication + linked wallet.
 *
 * Application status machine: DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED / REJECTED
 *
 * Email verification is handled by Clerk — the backend checks that the
 * submitted officialEmail is verified on the same Clerk user account at submit time.
 * No /verify-otp endpoint is needed.
 */
@RestController
@RequestMapping("/api/v1/ngo/applications")
@RequiredArgsConstructor
public class NgoApplicationController {

    private final NgoApplicationService applicationService;

    /**
     * Create a new NGO application in DRAFT state.
     * Wallet address is taken from the authenticated user's profile.
     * officialEmail is verified through Clerk when the application is submitted.
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApplicationRequest request) {
        String clerkId = jwt.getSubject();
        ApplicationResponse response = applicationService.createApplication(clerkId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit a DRAFT application for review.
     * Transitions directly to AI_SCREENING (email already verified by Clerk).
     */
    @PostMapping("/me/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();
        ApplicationResponse response = applicationService.submitApplication(clerkId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current user's most recent application (including terminal states).
     */
    @GetMapping("/me")
    public ResponseEntity<ApplicationResponse> getMyApplication(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();
        ApplicationResponse response = applicationService.getMyApplication(clerkId);
        return ResponseEntity.ok(response);
    }
}
