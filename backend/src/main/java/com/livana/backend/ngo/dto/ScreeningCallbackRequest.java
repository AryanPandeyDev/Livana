package com.livana.backend.ngo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload the AI screening service POSTs back to {@code POST /api/v1/internal/screening-callback}.
 *
 * <p>Bean validation mirrors the DB contract: {@code ai_confidence_score DECIMAL(5,2)} and
 * {@code ai_verdict} restricted to {@code PASS}/{@code FAIL}. A violation yields a
 * {@code 400 VALIDATION_ERROR} via the global exception handler.
 */
public record ScreeningCallbackRequest(
        @NotNull UUID applicationId,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal confidenceScore,
        @NotBlank String researchSummary,
        @NotNull @Pattern(regexp = "PASS|FAIL") String verdict
) {}
