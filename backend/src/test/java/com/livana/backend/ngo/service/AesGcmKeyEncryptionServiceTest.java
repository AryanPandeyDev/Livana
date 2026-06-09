package com.livana.backend.ngo.service;

import com.livana.backend.common.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the AES-GCM encryption of the admin-managed Gemini API key.
 *
 * Security-critical, pure logic — a broken round-trip would silently corrupt
 * every stored key, and a leaky mask would expose it. No Spring context needed;
 * the service is constructed directly with a base64 256-bit key.
 *
 * Production behaviors protected:
 * - encrypt → decrypt recovers the exact original (round-trip).
 * - ciphertext is never equal to the plaintext.
 * - each encryption uses a fresh IV (same input → different ciphertext).
 * - tampered ciphertext / wrong key fails closed (GCM auth tag).
 * - masking never reveals the full key.
 * - a missing encryption key fails closed rather than operating in the clear.
 */
class AesGcmKeyEncryptionServiceTest {

    /** A valid base64-encoded 256-bit (32-byte) AES key. */
    private static final String KEY_256 =
            Base64.getEncoder().encodeToString(new byte[32]);

    private static AesGcmKeyEncryptionService withKey(String base64Key) {
        return new AesGcmKeyEncryptionService(base64Key);
    }

    @Nested
    @DisplayName("encrypt / decrypt round-trip")
    class RoundTrip {

        @Test
        @DisplayName("decrypt(encrypt(k)) returns the exact original key")
        void roundTripRecoversOriginal() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String plaintext = "AIzaSyD-ExampleGeminiApiKey-1234567890abcdef";

            String encrypted = svc.encrypt(plaintext);
            String decrypted = svc.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("ciphertext is never equal to the plaintext")
        void ciphertextDiffersFromPlaintext() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String plaintext = "AIzaSecret";

            assertThat(svc.encrypt(plaintext)).isNotEqualTo(plaintext);
        }

        @Test
        @DisplayName("encrypting the same value twice yields different ciphertext (fresh IV)")
        void freshIvPerEncryption() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String plaintext = "same-input";

            String first = svc.encrypt(plaintext);
            String second = svc.encrypt(plaintext);

            // Different ciphertext...
            assertThat(first).isNotEqualTo(second);
            // ...but both decrypt back to the same plaintext.
            assertThat(svc.decrypt(first)).isEqualTo(plaintext);
            assertThat(svc.decrypt(second)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("handles unicode and long keys")
        void roundTripUnicodeAndLong() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String plaintext = "クレ-key-\uD83D\uDD11-" + "x".repeat(500);

            assertThat(svc.decrypt(svc.encrypt(plaintext))).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("tamper / wrong-key rejection")
    class FailClosed {

        @Test
        @DisplayName("tampered ciphertext fails decryption (GCM auth tag)")
        void tamperedCiphertextRejected() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String encrypted = svc.encrypt("secret");

            // Flip a character in the middle of the base64 ciphertext.
            char[] chars = encrypted.toCharArray();
            int mid = chars.length / 2;
            chars[mid] = (chars[mid] == 'A') ? 'B' : 'A';
            String tampered = new String(chars);

            assertThatThrownBy(() -> svc.decrypt(tampered))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                            .isEqualTo("AI_CONFIG_DECRYPT_ERROR"));
        }

        @Test
        @DisplayName("decrypting with a different key fails")
        void wrongKeyRejected() {
            byte[] otherKeyBytes = new byte[32];
            otherKeyBytes[0] = 1; // differs from the all-zero KEY_256
            String otherKey = Base64.getEncoder().encodeToString(otherKeyBytes);

            String encrypted = withKey(KEY_256).encrypt("secret");

            assertThatThrownBy(() -> withKey(otherKey).decrypt(encrypted))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                            .isEqualTo("AI_CONFIG_DECRYPT_ERROR"));
        }

        @Test
        @DisplayName("garbage input fails decryption rather than returning junk")
        void garbageInputRejected() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);

            assertThatThrownBy(() -> svc.decrypt("not-valid-base64-or-ciphertext!!!"))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    @DisplayName("missing encryption key fails closed")
    class MissingKey {

        @Test
        @DisplayName("encrypt with a blank encryption key throws KEY_MISSING")
        void encryptWithoutKeyThrows() {
            AesGcmKeyEncryptionService svc = withKey("");

            assertThatThrownBy(() -> svc.encrypt("secret"))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                            .isEqualTo("AI_CONFIG_KEY_MISSING"));
        }

        @Test
        @DisplayName("decrypt with a blank encryption key throws KEY_MISSING")
        void decryptWithoutKeyThrows() {
            AesGcmKeyEncryptionService svc = withKey("   ");

            assertThatThrownBy(() -> svc.decrypt("anything"))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                            .isEqualTo("AI_CONFIG_KEY_MISSING"));
        }
    }

    @Nested
    @DisplayName("mask")
    class Mask {

        @Test
        @DisplayName("masks the middle of a long key, keeping only a short prefix/suffix")
        void masksLongKey() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String key = "AIzaSyD1234567890abcdefXYZ";

            String masked = svc.mask(key);

            // The full key must never appear in the masked form.
            assertThat(masked).doesNotContain(key);
            // Keeps the first 4 characters as a hint.
            assertThat(masked).startsWith("AIza");
            // The bulk of the key (its middle) must not leak.
            assertThat(masked).doesNotContain("567890abcdef");
        }

        @Test
        @DisplayName("fully masks a short key so nothing meaningful leaks")
        void fullyMasksShortKey() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            String shortKey = "abc123"; // <= prefix(4)+suffix(3)

            String masked = svc.mask(shortKey);

            assertThat(masked).doesNotContain(shortKey);
        }

        @Test
        @DisplayName("null is masked without throwing")
        void masksNull() {
            AesGcmKeyEncryptionService svc = withKey(KEY_256);
            assertThat(svc.mask(null)).isNotNull();
        }
    }
}
