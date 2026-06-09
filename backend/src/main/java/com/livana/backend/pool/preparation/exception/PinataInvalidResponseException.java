package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PinataInvalidResponseException extends ApiException {
    public PinataInvalidResponseException(String reason) {
        super(HttpStatus.BAD_GATEWAY, "PINATA_INVALID_RESPONSE", reason);
    }
}
