package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class NgoNotApprovedException extends ApiException {
    public NgoNotApprovedException() {
        super(HttpStatus.FORBIDDEN, "NGO_NOT_APPROVED",
                "Authenticated wallet has no approved NGO application");
    }
}
