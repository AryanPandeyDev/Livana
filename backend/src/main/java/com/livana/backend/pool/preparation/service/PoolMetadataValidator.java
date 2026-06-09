package com.livana.backend.pool.preparation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.livana.backend.common.exception.ApiException;
import com.livana.backend.pool.preparation.dto.PreparePoolRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validates a JsonNode body against the Pool_Metadata_Schema, mirroring the
 * rules in IpfsMetadataService.parseAndValidate. Stricter on type checks
 * (rejects non-integer targetAmount values that the indexer would silently
 * coerce to 0) — safe direction since rejecting more inputs cannot cause a
 * pool to be silently dropped.
 */
@Component
public class PoolMetadataValidator {

    public PreparePoolRequest validate(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw validationError("request body must be a JSON object");
        }
        String title = requireNonBlankString(body, "title");
        String description = requireNonBlankString(body, "description");
        String region = requireNonBlankString(body, "region");
        long targetAmount = requirePositiveLong(body, "targetAmount");
        String coverImage = optionalNonBlankString(body, "coverImage");
        return new PreparePoolRequest(title, description, region, coverImage, targetAmount);
    }

    private static String requireNonBlankString(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            throw validationError(field + " is required");
        }
        if (!node.isTextual()) {
            throw validationError(field + " must be a string");
        }
        String text = node.asText();
        if (text.isBlank()) {
            throw validationError(field + " must not be blank");
        }
        return text;
    }

    private static long requirePositiveLong(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            throw validationError(field + " is required");
        }
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw validationError(field + " must be a JSON integer in long range");
        }
        long value = node.asLong();
        if (value <= 0) {
            throw validationError(field + " must be greater than 0");
        }
        return value;
    }

    private static String optionalNonBlankString(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) return null;
        if (!node.isTextual()) {
            throw validationError(field + " must be a string when present");
        }
        String text = node.asText();
        if (text.isBlank()) {
            throw validationError(field + " must not be blank when present");
        }
        return text;
    }

    private static ApiException validationError(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
