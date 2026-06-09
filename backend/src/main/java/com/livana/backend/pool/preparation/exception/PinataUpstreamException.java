package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PinataUpstreamException extends ApiException {
    public PinataUpstreamException() {
        super(HttpStatus.BAD_GATEWAY, "PINATA_UPSTREAM_ERROR",
                "Pinata returned an upstream error");
    }
}
