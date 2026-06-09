package com.livana.backend.auth.service;

import com.livana.backend.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages wallet-linking challenges (nonces) to prevent replay attacks.
 *
 * Flow:
 * 1. GET /api/v1/users/me/wallet/challenge → generates a nonce, binds to clerkId, returns message
 * 2. User signs the message with their wallet
 * 3. PATCH /api/v1/users/me/wallet → submits signature, backend verifies nonce was issued to this user
 * 4. Nonce is consumed (one-time use) and expired nonces are rejected
 */
@Service
@Slf4j
public class WalletChallengeService {

    private static final long CHALLENGE_TTL_SECONDS = 300; // 5 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // In production, use Redis or DB. For a single-instance backend this is fine.
    private final Map<String, Challenge> activeChallenges = new ConcurrentHashMap<>();

    private record Challenge(String nonce, String clerkId, Instant expiresAt) {}

    /**
     * Expired challenges are invalid even before this runs; this just keeps the
     * in-memory store from growing with abandoned challenges.
     */
    @Scheduled(fixedRate = 60_000)
    void cleanupExpiredChallenges() {
        Instant now = Instant.now();
        activeChallenges.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    /**
     * Generate a challenge message bound to the authenticated user.
     * Returns the full message that the user must sign.
     */
    public String generateChallenge(String clerkId) {
        // Generate cryptographic nonce
        byte[] nonceBytes = new byte[32];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        Instant expiresAt = Instant.now().plusSeconds(CHALLENGE_TTL_SECONDS);
        activeChallenges.put(clerkId, new Challenge(nonce, clerkId, expiresAt));

        // Build the message the user will sign
        return buildMessage(nonce);
    }

    /**
     * Verify that a challenge message was issued to this user and is still valid.
     * Consumes the challenge (one-time use).
     */
    public void verifyAndConsume(String clerkId, String message) {
        Challenge challenge = activeChallenges.get(clerkId);

        if (challenge == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_CHALLENGE",
                    "No active wallet challenge. Call GET /api/v1/users/me/wallet/challenge first.");
        }

        if (Instant.now().isAfter(challenge.expiresAt())) {
            activeChallenges.remove(clerkId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_EXPIRED",
                    "Challenge has expired. Request a new one.");
        }

        // Verify the signed message matches what we issued
        String expectedMessage = buildMessage(challenge.nonce());
        if (!expectedMessage.equals(message)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_MISMATCH",
                    "Signed message does not match the issued challenge.");
        }

        // Consume — can't be reused
        activeChallenges.remove(clerkId);
    }

    private String buildMessage(String nonce) {
        return "Livana Wallet Verification\n\n" +
                "Sign this message to prove you own this wallet.\n" +
                "This signature will not trigger a blockchain transaction.\n\n" +
                "Nonce: " + nonce;
    }
}
