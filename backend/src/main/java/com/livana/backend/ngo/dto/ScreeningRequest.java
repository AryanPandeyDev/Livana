package com.livana.backend.ngo.dto;

import java.util.UUID;

/**
 * Payload the backend sends to the AI screening service via {@code POST /screen}.
 *
 * <p>The cross-codebase JSON contract; the Python {@code ScreenRequest} model mirrors this.
 * The {@code geminiApiKey} is injected at trigger time (not persisted on the entity) and is
 * never logged.
 */
public record ScreeningRequest(
        UUID applicationId,
        String orgName,
        String registrationNumber,
        String description,
        String officialEmail,
        String documentsCid,
        String geminiApiKey
) {
    /** Return a copy with the Gemini key set (key is injected at trigger time, not stored on the entity). */
    public ScreeningRequest withGeminiKey(String key) {
        return new ScreeningRequest(applicationId, orgName, registrationNumber,
                description, officialEmail, documentsCid, key);
    }
}
