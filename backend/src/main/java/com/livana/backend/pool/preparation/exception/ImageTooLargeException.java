package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ImageTooLargeException extends ApiException {
    public ImageTooLargeException(long maxBytes) {
        super(HttpStatus.BAD_REQUEST, "IMAGE_TOO_LARGE",
                "Image exceeds maximum allowed size of " + maxBytes + " bytes");
    }
}
