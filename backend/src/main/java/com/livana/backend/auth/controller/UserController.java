package com.livana.backend.auth.controller;

import com.livana.backend.auth.dto.LinkWalletRequest;
import com.livana.backend.auth.dto.UserProfileResponse;
import com.livana.backend.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();
        UserProfileResponse profile = userService.getProfile(clerkId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Generate a wallet-linking challenge.
     * Returns a message that the frontend must ask the user to sign with their wallet.
     */
    @GetMapping("/me/wallet/challenge")
    public ResponseEntity<Map<String, String>> getWalletChallenge(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();
        String message = userService.generateWalletChallenge(clerkId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Link a wallet to the user's profile.
     * Requires a valid challenge signature proving wallet ownership.
     */
    @PatchMapping("/me/wallet")
    public ResponseEntity<UserProfileResponse> linkWallet(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LinkWalletRequest request) {
        String clerkId = jwt.getSubject();
        UserProfileResponse profile = userService.linkWallet(clerkId, request);
        return ResponseEntity.ok(profile);
    }
}
