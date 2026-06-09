package com.livana.backend.pool.preparation.exception;

import com.livana.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ImageTypeNotAllowedException extends ApiException {
    public ImageTypeNotAllowedException(String actualContentType) {
        super(HttpStatus.BAD_REQUEST, "IMAGE_TYPE_NOT_ALLOWED",
                "Image content type '" + actualContentType
                        + "' is not allowed. Allowed types: image/jpeg, image/png, image/webp");
    }
}
