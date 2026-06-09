package com.livana.backend.auth.service;

import com.livana.backend.auth.dto.LinkWalletRequest;
import com.livana.backend.auth.dto.UserProfileResponse;
import com.livana.backend.auth.entity.User;
import com.livana.backend.auth.repository.UserRepository;
import com.livana.backend.common.exception.ApiException;
import com.livana.backend.common.exception.WalletNotLinkedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SignatureVerificationService signatureVerificationService;
    private final WalletChallengeService walletChallengeService;

    /**
     * Get current user's profile by their Clerk ID.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return toProfileResponse(user);
    }

    /**
     * Generate a wallet-linking challenge bound to the authenticated user.
     * Returns the message the frontend must ask the user to sign.
     */
    public String generateWalletChallenge(String clerkId) {
        return walletChallengeService.generateChallenge(clerkId);
    }

    /**
     * Link a wallet address to the authenticated user's profile.
     *
     * Security:
     * 1. Verifies the signed message matches the challenge we issued (prevents replay)
     * 2. Recovers the signer via EIP-191 ecrecover (proves wallet ownership)
     * 3. Catches DB unique constraint violation (handles race condition)
     *
     * Wallet is stored lowercase (design principle #2 from schema doc).
     */
    @Transactional
    public UserProfileResponse linkWallet(String clerkId, LinkWalletRequest request) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String normalizedAddress = request.walletAddress().toLowerCase();

        // Verify the message matches the challenge we issued to this user
        walletChallengeService.verifyAndConsume(clerkId, request.message());

        // Verify wallet ownership via EIP-191 signature recovery
        boolean isOwner = signatureVerificationService.verifyPersonalSign(
                normalizedAddress, request.message(), request.signature());
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SIGNATURE_INVALID",
                    "Signature does not match the claimed wallet address");
        }

        user.setWalletAddress(normalizedAddress);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: DB unique constraint caught a concurrent link
            throw new ApiException(HttpStatus.CONFLICT, "WALLET_ALREADY_LINKED",
                    "This wallet address is already linked to another account");
        }

        log.info("Linked wallet {} to user clerkId={}", normalizedAddress, clerkId);
        return toProfileResponse(user);
    }

    /**
     * Get user by Clerk ID, throwing if not found.
     * Also checks that wallet is linked — throws WALLET_NOT_LINKED if not.
     *
     * From PRD: "Endpoints that require an on-chain action check that the
     * authenticated user has a walletAddress linked. If not, they return 403
     * with error code WALLET_NOT_LINKED."
     */
    @Transactional(readOnly = true)
    public User getAuthenticatedUserWithWallet(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getWalletAddress() == null) {
            throw new WalletNotLinkedException();
        }
        return user;
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .walletAddress(user.getWalletAddress())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
