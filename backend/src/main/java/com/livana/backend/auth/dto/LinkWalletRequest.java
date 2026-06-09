package com.livana.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to link a wallet via signed message verification.
 *
 * Flow:
 * 1. Frontend calls GET /api/v1/users/me/wallet/challenge to get a nonce
 * 2. User signs the nonce message with their wallet (personal_sign)
 * 3. Frontend sends wallet address + signature to PATCH /api/v1/users/me/wallet
 * 4. Backend recovers the signer from the signature and verifies it matches the wallet
 * 5. Only then is the wallet persisted
 */
public record LinkWalletRequest(
        @NotBlank(message = "Wallet address is required")
        @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum wallet address")
        String walletAddress,

        @NotBlank(message = "Signature is required")
        @Pattern(regexp = "^0x[a-fA-F0-9]{130}$", message = "Invalid signature format")
        String signature,

        @NotBlank(message = "Message is required")
        String message
) {}
