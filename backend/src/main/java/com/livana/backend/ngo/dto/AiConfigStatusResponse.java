package com.livana.backend.ngo.dto;

import java.time.OffsetDateTime;

/**
 * Masked AI config status for admins. Never contains the raw Gemini key.
 * When no key is configured, configured=false and maskedKey/setBy/setAt are null.
 */
public record AiConfigStatusResponse(
        boolean configured,
        String maskedKey,
        String setBy,
        OffsetDateTime setAt
) {}
