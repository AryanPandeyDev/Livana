package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PinataKeyRequiredException extends ApiException {
    public PinataKeyRequiredException(String headerName) {
        super(HttpStatus.BAD_REQUEST, "PINATA_KEY_REQUIRED",
                "Required header missing or blank: " + headerName);
    }
}
