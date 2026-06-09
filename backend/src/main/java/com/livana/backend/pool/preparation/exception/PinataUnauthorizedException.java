package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PinataUnauthorizedException extends ApiException {
    public PinataUnauthorizedException() {
        super(HttpStatus.BAD_REQUEST, "PINATA_UNAUTHORIZED",
                "Pinata rejected the supplied API key");
    }
}
