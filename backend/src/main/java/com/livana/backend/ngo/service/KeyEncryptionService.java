package com.livana.backend.ngo.service;

/**
 * Symmetric encryption for sensitive operational secrets (the admin-managed
 * Gemini API key). Backed by AES-GCM authenticated encryption with a 256-bit
 * key sourced from configuration ({@code ai-config.encryption-key}).
 *
 * <p>The key material itself never lives in the database — only the ciphertext
 * produced by {@link #encrypt(String)} is persisted. Implementations must never
 * log plaintext or key material.
 */
public interface KeyEncryptionService {

    /**
     * Encrypt the given plaintext.
     *
     * @param plaintext the value to encrypt
     * @return base64 encoding of {@code (iv || ciphertext+tag)} using a fresh
     *         random 12-byte IV per call
     */
    String encrypt(String plaintext);

    /**
     * Inverse of {@link #encrypt(String)}.
     *
     * @param stored the base64 {@code (iv || ciphertext+tag)} value produced by encrypt
     * @return the recovered plaintext
     * @throws com.livana.backend.common.exception.ApiException on tamper, wrong
     *         key, or any decryption failure (GCM auth tag mismatch)
     */
    String decrypt(String stored);

    /**
     * Produce a display-safe masked form of a secret.
     *
     * <p>For values longer than the prefix + suffix length, keeps the leading 4
     * and trailing 3 characters with a fixed mask in between (e.g. {@code AIza...3f9}).
     * Shorter values are fully masked so no meaningful characters of the original leak.
     *
     * @param plaintext the secret to mask
     * @return the masked display form; never the raw value
     */
    String mask(String plaintext);
}
