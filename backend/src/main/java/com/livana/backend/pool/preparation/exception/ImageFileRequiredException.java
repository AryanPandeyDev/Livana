package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ImageFileRequiredException extends ApiException {
    public ImageFileRequiredException() {
        super(HttpStatus.BAD_REQUEST, "IMAGE_FILE_REQUIRED",
                "A non-empty file part named 'file' is required");
    }
}
