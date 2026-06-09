package com.livana.backend.common.validation;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/**
 * Static utility for validating and normalizing Ethereum addresses.
 */
public final class AddressValidator {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private AddressValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that the given address is a 42-character hex string starting with "0x"
     * and returns it lowercased.
     *
     * @param address   the Ethereum address to validate
     * @param paramName the parameter name (used in error messages)
     * @return the address lowercased
     * @throws ApiException if the address is null, blank, or not a valid format
     */
    public static String validateAndNormalize(String address, String paramName) {
        if (address == null || address.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ADDRESS",
                    paramName + " must not be empty");
        }
        if (!ADDRESS_PATTERN.matcher(address).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ADDRESS",
                    paramName + " must be a 42-character hex string starting with 0x");
        }
        return address.toLowerCase();
    }
}
