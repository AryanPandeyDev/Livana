package com.livana.backend.pool.preparation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.pool.preparation.exception.PinataInvalidResponseException;
import com.livana.backend.pool.preparation.exception.PinataRejectedException;
import com.livana.backend.pool.preparation.exception.PinataUnauthorizedException;
import com.livana.backend.pool.preparation.exception.PinataUnreachableException;
import com.livana.backend.pool.preparation.exception.PinataUpstreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Pinata HTTP client. Single-purpose @Service that owns all Pinata API calls
 * and the mapping from upstream HTTP outcomes to our Pinata-specific exceptions.
 *
 * <p>Logging discipline: this class only ever logs the target URL, the HTTP
 * status code, and (on success) the returned IPFS CID. It never logs header
 * values, request bodies, or response bodies — see Requirement 7.6 / 7.7.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PinataClient {

    private static final URI PIN_FILE_URL = URI.create("https://api.pinata.cloud/pinning/pinFileToIPFS");
    private static final URI PIN_JSON_URL = URI.create("https://api.pinata.cloud/pinning/pinJSONToIPFS");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Pin a binary file (image) to IPFS via Pinata's pinFileToIPFS endpoint.
     *
     * @param keys the per-request Pinata credentials (forwarded verbatim, never logged)
     * @param file the multipart file to upload
     * @return the IPFS CID returned by Pinata
     */
    public String pinFile(PinataHeaders keys, MultipartFile file) {
        HttpRequest request = buildMultipartRequest(PIN_FILE_URL, keys, file);
        return send(PIN_FILE_URL, request);
    }

    /**
     * Pin a canonical JSON document to IPFS via Pinata's pinJSONToIPFS endpoint.
     *
     * <p>Pinata's {@code pinJSONToIPFS} endpoint pins the JSON object held inside
     * the {@code pinataContent} field of the request body — not the raw request
     * body itself. We therefore wrap the canonical metadata bytes inside a
     * {@code pinataContent} envelope before sending.
     *
     * @param keys           the per-request Pinata credentials
     * @param canonicalJson  pre-serialized JSON bytes of the metadata to pin
     *                       (must itself be a valid JSON value)
     * @return the IPFS CID returned by Pinata
     */
    public String pinJson(PinataHeaders keys, byte[] canonicalJson) {
        byte[] envelope = wrapInPinataContent(canonicalJson);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(PIN_JSON_URL)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(envelope));
        keys.applyTo(builder);
        return send(PIN_JSON_URL, builder.build());
    }

    /**
     * Wraps the canonical metadata JSON in Pinata's required envelope:
     * {@code {"pinataContent": <metadata>}}. The metadata bytes are inserted
     * verbatim so the canonical form (field order, integer literals) is preserved
     * exactly — the indexer reads back the same content from IPFS.
     */
    private byte[] wrapInPinataContent(byte[] canonicalJson) {
        byte[] prefix = "{\"pinataContent\":".getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "}".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(
                prefix.length + canonicalJson.length + suffix.length);
        try {
            out.write(prefix);
            out.write(canonicalJson);
            out.write(suffix);
        } catch (IOException e) {
            throw new IllegalStateException("Unreachable: ByteArrayOutputStream.write", e);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private HttpRequest buildMultipartRequest(URI url, PinataHeaders keys, MultipartFile file) {
        String boundary = "----LivanaBoundary" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, file);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(url)
                .timeout(TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        keys.applyTo(builder);
        return builder.build();
    }

    /**
     * Build an RFC 7578 multipart/form-data body containing exactly one part
     * named "file". Format:
     *
     * <pre>
     * --&lt;boundary&gt;\r\n
     * Content-Disposition: form-data; name="file"; filename="&lt;name&gt;"\r\n
     * Content-Type: &lt;content-type&gt;\r\n
     * \r\n
     * &lt;file-bytes&gt;\r\n
     * --&lt;boundary&gt;--\r\n
     * </pre>
     *
     * <p>The original filename is sanitized — CR/LF/NUL are stripped and any
     * embedded quotes/backslashes are escaped — so a hostile filename cannot
     * inject extra header lines or terminate the boundary prematurely.
     */
    private byte[] buildMultipartBody(String boundary, MultipartFile file) {
        String filename = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "\r\n";
        String trailer = "\r\n--" + boundary + "--\r\n";

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            // Unable to read the multipart payload that was already buffered by Spring.
            // Treat as an upstream-side failure so the caller surfaces a 502 rather than 500.
            throw new PinataUnreachableException("Failed to read uploaded file bytes");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(
                header.length() + fileBytes.length + trailer.length());
        try {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(trailer.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ByteArrayOutputStream.write does not throw in practice; rethrow defensively.
            throw new PinataUnreachableException("Failed to assemble multipart body");
        }
        return out.toByteArray();
    }

    /**
     * Make a user-supplied filename safe to embed in a {@code Content-Disposition}
     * header. Strips CR/LF/NUL (which would terminate the header line and let an
     * attacker forge a new MIME part) and escapes backslashes and double quotes
     * (RFC 6266 quoted-string semantics). Falls back to "image" if the filename
     * is null or empty after stripping.
     */
    private static String sanitizeFilename(String original) {
        if (original == null) {
            return "image";
        }
        // Remove CR, LF, NUL, and any other ASCII control characters
        StringBuilder sb = new StringBuilder(original.length());
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                continue;
            }
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        String stripped = sb.toString().trim();
        return stripped.isEmpty() ? "image" : stripped;
    }

    private String send(URI url, HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            log.warn("Pinata request to {} timed out", url);
            throw new PinataUnreachableException("Pinata request timed out");
        } catch (IOException e) {
            log.warn("Pinata request to {} failed: {}", url, e.getClass().getSimpleName());
            throw new PinataUnreachableException("Pinata request failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Pinata request to {} failed: {}", url, e.getClass().getSimpleName());
            throw new PinataUnreachableException("Pinata request failed");
        }

        int status = response.statusCode();
        if (status == 200) {
            String cid = extractIpfsHash(response.body());
            log.info("Pinata pin succeeded url={} status=200 cid={}", url, cid);
            return cid;
        }
        log.info("Pinata pin failed url={} status={}", url, status);
        if (status == 401 || status == 403) {
            throw new PinataUnauthorizedException();
        }
        if (status >= 400 && status < 500) {
            throw new PinataRejectedException(status);
        }
        if (status >= 500 && status < 600) {
            throw new PinataUpstreamException();
        }
        throw new PinataInvalidResponseException("Unexpected status " + status);
    }

    private String extractIpfsHash(String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new PinataInvalidResponseException("Malformed Pinata response");
        }
        JsonNode hash = root.get("IpfsHash");
        if (hash == null || !hash.isTextual() || hash.asText().isBlank()) {
            throw new PinataInvalidResponseException("Missing or blank IpfsHash");
        }
        return hash.asText();
    }
}
