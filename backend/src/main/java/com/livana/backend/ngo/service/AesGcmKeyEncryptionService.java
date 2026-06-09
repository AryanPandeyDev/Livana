package com.livana.backend.ngo.service;

import com.livana.backend.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-GCM implementation of {@link KeyEncryptionService}.
 *
 * <p>Uses a 256-bit key (base64-encoded in {@code ai-config.encryption-key}),
 * {@code AES/GCM/NoPadding}, a fresh random 12-byte IV per encryption, and a
 * 128-bit authentication tag. The stored form is {@code base64(iv || ciphertext+tag)}
 * so a single column round-trips cleanly.
 *
 * <p>GCM is an authenticated mode: any tampering with the ciphertext, or use of
 * the wrong key, causes decryption to fail with an authentication error. Failures
 * surface as {@link ApiException} without exposing key material.
 */
@Service
@Slf4j
public class AesGcmKeyEncryptionService implements KeyEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final int MASK_PREFIX = 4;
    private static final int MASK_SUFFIX = 3;
    private static final String MASK_SEPARATOR = "...";
    private static final String FULLY_MASKED = "*******";

    private static final String DECRYPT_ERROR_CODE = "AI_CONFIG_DECRYPT_ERROR";
    private static final String KEY_MISSING_CODE = "AI_CONFIG_KEY_MISSING";

    private final SecureRandom secureRandom = new SecureRandom();

    /** Base64-encoded 256-bit AES key, or blank when unconfigured. */
    private final String encodedKey;

    public AesGcmKeyEncryptionService(@Value("${ai-config.encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    @Override
    public String encrypt(String plaintext) {
        SecretKeySpec key = loadKey();
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Do not include key material or plaintext in the message.
            log.error("Encryption failed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, DECRYPT_ERROR_CODE,
                    "Failed to encrypt configuration value.");
        }
    }

    @Override
    public String decrypt(String stored) {
        SecretKeySpec key = loadKey();
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Stored value too short to contain IV and ciphertext");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);

            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Covers tamper (auth tag mismatch), wrong key, and malformed input.
            // Do not include key material or ciphertext in the message.
            log.error("Decryption failed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, DECRYPT_ERROR_CODE,
                    "Failed to decrypt configuration value.");
        }
    }

    @Override
    public String mask(String plaintext) {
        if (plaintext == null) {
            return FULLY_MASKED;
        }
        if (plaintext.length() > MASK_PREFIX + MASK_SUFFIX) {
            String prefix = plaintext.substring(0, MASK_PREFIX);
            String suffix = plaintext.substring(plaintext.length() - MASK_SUFFIX);
            return prefix + MASK_SEPARATOR + suffix;
        }
        // Short values: fully masked so no meaningful characters of the original leak.
        return FULLY_MASKED;
    }

    /**
     * Decode the configured base64 key into a {@link SecretKeySpec}.
     *
     * @throws ApiException if the key is unconfigured (blank) or undecodable.
     */
    private SecretKeySpec loadKey() {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, KEY_MISSING_CODE,
                    "Encryption key not configured");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey.trim());
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (IllegalArgumentException e) {
            // Malformed key configuration — never echo the key value.
            log.error("Configured encryption key is not valid base64");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, KEY_MISSING_CODE,
                    "Encryption key not configured");
        }
    }
}
