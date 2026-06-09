package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PinataUnreachableException extends ApiException {
    public PinataUnreachableException(String reason) {
        super(HttpStatus.BAD_GATEWAY, "PINATA_UNREACHABLE", reason);
    }
}
