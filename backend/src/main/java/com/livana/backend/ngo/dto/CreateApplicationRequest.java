package com.livana.backend.ngo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new NGO application (DRAFT state).
 * The wallet_address is taken from the authenticated user's linked wallet — not from the request body.
 */
public record CreateApplicationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 255, message = "Organization name must be at most 255 characters")
        String orgName,

        @NotBlank(message = "Registration number is required")
        @Size(max = 100, message = "Registration number must be at most 100 characters")
        String registrationNumber,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Official email is required")
        @Email(message = "Official email must be a valid email address")
        String officialEmail,

        String documentsCid
) {}
