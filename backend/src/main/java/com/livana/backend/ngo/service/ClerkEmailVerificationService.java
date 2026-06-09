package com.livana.backend.ngo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies that an email address is a verified email on a Clerk user account.
 *
 * Calls the Clerk Backend API:
 *   GET https://api.clerk.com/v1/users/{clerk_id}
 *   Authorization: Bearer {CLERK_SECRET_KEY}
 *
 * Inspects the user's email_addresses array for a matching email with
 * verification.status == "verified".
 *
 * This is used during NGO application submission to confirm that the
 * applicant controls the officialEmail they provided. The officialEmail
 * may differ from the user's primary Clerk login email — the frontend
 * guides the applicant to add and verify it through Clerk's flow first.
 */
@Service
@Slf4j
public class ClerkEmailVerificationService {

    private static final String CLERK_API_BASE = "https://api.clerk.com/v1";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String clerkSecretKey;
    private final ObjectMapper objectMapper;

    public ClerkEmailVerificationService(
            @Value("${clerk.secret-key:}") String clerkSecretKey,
            ObjectMapper objectMapper) {
        this.clerkSecretKey = clerkSecretKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Check whether the given email is a verified email address on the
     * Clerk user identified by clerkId.
     *
     * @param clerkId       the Clerk user ID (e.g. "user_2abc123")
     * @param officialEmail the email address to check
     * @return true if the email is found and verified on this Clerk user
     * @throws ApiException if the Clerk API call fails or the secret key is missing
     */
    public boolean isVerifiedEmailForUser(String clerkId, String officialEmail) {
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CLERK_CONFIG_ERROR",
                    "Clerk secret key is not configured. Set CLERK_SECRET_KEY environment variable.");
        }

        String url = CLERK_API_BASE + "/users/" + clerkId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + clerkSecretKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                log.error("Clerk API auth failed (HTTP {}). Check CLERK_SECRET_KEY.", response.statusCode());
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CLERK_AUTH_ERROR",
                        "Clerk API authentication failed. Contact platform admin.");
            }

            if (response.statusCode() == 404) {
                log.error("Clerk user not found: clerkId={}", clerkId);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CLERK_USER_NOT_FOUND",
                        "Clerk user not found. This should not happen for an authenticated user.");
            }

            if (response.statusCode() != 200) {
                log.error("Clerk API returned HTTP {} for clerkId={}: {}",
                        response.statusCode(), clerkId, response.body());
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CLERK_EMAIL_CHECK_FAILED",
                        "Could not verify email with Clerk. Please try again later.");
            }

            return checkEmailVerified(response.body(), officialEmail);

        } catch (ApiException e) {
            throw e; // Re-throw our own exceptions
        } catch (Exception e) {
            log.error("Failed to call Clerk API for clerkId={}: {}", clerkId, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CLERK_EMAIL_CHECK_FAILED",
                    "Could not verify email with Clerk. Please try again later.");
        }
    }

    /**
     * Parse the Clerk user response and check if officialEmail is verified.
     *
     * Clerk response structure:
     * {
     *   "email_addresses": [
     *     {
     *       "email_address": "director@ngo.org",
     *       "verification": { "status": "verified", ... }
     *     }
     *   ]
     * }
     */
    private boolean checkEmailVerified(String responseBody, String officialEmail) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode emailAddresses = root.get("email_addresses");

            if (emailAddresses == null || !emailAddresses.isArray()) {
                log.warn("Clerk user response has no email_addresses array");
                return false;
            }

            for (JsonNode emailEntry : emailAddresses) {
                String address = emailEntry.has("email_address")
                        ? emailEntry.get("email_address").asText()
                        : null;

                if (address != null && address.equalsIgnoreCase(officialEmail)) {
                    // Found the email — check verification status
                    JsonNode verification = emailEntry.get("verification");
                    if (verification != null && "verified".equals(verification.path("status").asText())) {
                        return true;
                    }
                    // Email exists but not verified
                    log.debug("Email {} found on Clerk user but not verified (status={})",
                            officialEmail, verification != null ? verification.path("status").asText() : "null");
                    return false;
                }
            }

            // Email not found on this Clerk user
            log.debug("Email {} not found on Clerk user's email addresses", officialEmail);
            return false;

        } catch (Exception e) {
            log.error("Failed to parse Clerk API response: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CLERK_EMAIL_CHECK_FAILED",
                    "Could not verify email with Clerk. Please try again later.");
        }
    }
}
