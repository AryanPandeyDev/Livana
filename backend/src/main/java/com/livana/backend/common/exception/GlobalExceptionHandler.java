package com.livana.backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("API exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                details,
                OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Surfaces Spring's framework-level multipart rejection (request exceeds
     * spring.servlet.multipart.max-file-size or max-request-size) as the same
     * 400 IMAGE_TOO_LARGE used by {@code ImageValidator}.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Max upload size exceeded");
        ErrorResponse response = new ErrorResponse(
                "IMAGE_TOO_LARGE",
                "Image exceeds maximum allowed size of 5242880 bytes",
                OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Surfaces malformed JSON request bodies (e.g. invalid syntax, body that does
     * not parse as JSON) as 400 VALIDATION_ERROR rather than the generic 500
     * handled by {@link #handleGeneral}. Covers the {@code /api/v1/pools/prepare}
     * endpoint where Jackson parses the body into a {@code JsonNode} before our
     * validator runs.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request body: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                "Malformed or unreadable JSON request body",
                OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse response = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                OffsetDateTime.now()
        );
        return ResponseEntity.internalServerError().body(response);
    }

    public record ErrorResponse(String errorCode, String message, OffsetDateTime timestamp) {}
}
