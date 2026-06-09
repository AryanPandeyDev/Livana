package com.livana.backend.ngo.service;

import com.livana.backend.ngo.dto.AiConfigStatusResponse;
import com.livana.backend.ngo.entity.AiConfig;
import com.livana.backend.ngo.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Manages the single active AI configuration record holding the admin-managed
 * Gemini API key. The raw key is encrypted at rest via {@link KeyEncryptionService}
 * and never returned to callers — admins only ever see a masked form.
 */
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final KeyEncryptionService keyEncryptionService;

    /** Set/replace the Gemini key. Encrypts and stores as the single active record. */
    @Transactional
    public void setKey(String geminiApiKey, String setBy) {
        String ciphertext = keyEncryptionService.encrypt(geminiApiKey);
        AiConfig config = aiConfigRepository.findFirstByActiveTrue()
                .orElseGet(() -> AiConfig.builder().active(true).build());
        config.setEncryptedKey(ciphertext);
        config.setSetBy(setBy);
        config.setSetAt(OffsetDateTime.now());
        config.setActive(true);
        aiConfigRepository.save(config);
    }

    /** Masked status for admins. Never includes the raw key. */
    @Transactional(readOnly = true)
    public AiConfigStatusResponse getStatus() {
        return aiConfigRepository.findFirstByActiveTrue()
                .map(c -> new AiConfigStatusResponse(
                        true,
                        keyEncryptionService.mask(keyEncryptionService.decrypt(c.getEncryptedKey())),
                        c.getSetBy(),
                        c.getSetAt()))
                .orElse(new AiConfigStatusResponse(false, null, null, null));
    }

    /** Decrypted key for the screening trigger. Empty if not configured. */
    @Transactional(readOnly = true)
    public Optional<String> getDecryptedKey() {
        return aiConfigRepository.findFirstByActiveTrue()
                .map(c -> keyEncryptionService.decrypt(c.getEncryptedKey()));
    }
}
