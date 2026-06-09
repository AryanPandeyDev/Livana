package com.livana.backend.pool.preparation.service;

import com.livana.backend.pool.preparation.exception.ImageFileRequiredException;
import com.livana.backend.pool.preparation.exception.ImageTooLargeException;
import com.livana.backend.pool.preparation.exception.ImageTypeNotAllowedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Validates the cover image upload before it is forwarded to Pinata.
 *
 * Rules evaluated in order:
 *   1. Presence: file must be non-null and non-empty (size > 0)
 *   2. Content type: lowercased value must be in ALLOWED
 *   3. Size: bytes must be <= MAX_BYTES (5 MB)
 */
public final class ImageValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private ImageValidator() {
        // utility
    }

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageFileRequiredException();
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ImageTypeNotAllowedException(contentType);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ImageTooLargeException(MAX_BYTES);
        }
    }
}
