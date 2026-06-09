package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PinataRejectedException extends ApiException {

    private final int upstreamStatus;

    public PinataRejectedException(int upstreamStatus) {
        super(HttpStatus.BAD_REQUEST, "PINATA_REJECTED_REQUEST",
                "Pinata rejected the request with status " + upstreamStatus);
        this.upstreamStatus = upstreamStatus;
    }
}
