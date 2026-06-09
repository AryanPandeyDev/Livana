package com.livana.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Authenticates machine-to-machine calls on /api/v1/internal/** using a shared
 * secret in the X-Internal-Secret header. Constant-time comparison. On mismatch
 * writes a 401 ErrorResponse-shaped body and stops the chain.
 */
public class InternalSecretFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Secret";
    private final String expectedSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalSecretFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret == null ? "" : expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, expectedSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            var body = Map.of(
                    "errorCode", "UNAUTHORIZED",
                    "message", "Invalid or missing internal secret",
                    "timestamp", OffsetDateTime.now().toString());
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
