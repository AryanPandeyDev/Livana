package com.livana.backend.ngo.dto;

import jakarta.validation.constraints.NotBlank;

public record SetAiConfigRequest(
        @NotBlank(message = "Gemini API key is required")
        String geminiApiKey
) {}
