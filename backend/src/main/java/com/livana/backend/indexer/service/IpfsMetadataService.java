package com.livana.backend.indexer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.indexer.config.IndexerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches and validates pool metadata from IPFS.
 *
 * From PRD:
 * - "Pool metadata is stored on IPFS, with the CID stored on-chain in the FundPool contract's metadataCid field"
 * - "The metadata JSON schema is: { title, description, region, coverImage (IPFS CID), targetAmount }"
 * - "If the metadata CID is invalid [...] the pool is not indexed at all"
 *
 * Important distinction: transient failures (gateway down, timeout) must NOT be
 * treated the same as permanently invalid metadata. A Pinata outage should not
 * permanently hide a valid pool.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpfsMetadataService {

    private final IndexerProperties properties;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ========================================================================
    // Result types
    // ========================================================================

    /** Parsed pool metadata from IPFS. */
    public record PoolMetadata(
            String title,
            String description,
            String region,
            String coverImage,
            long targetAmount
    ) {}

    /** Discriminated result: VALID (with data), INVALID (permanent), or TRANSIENT_FAILURE. */
    public sealed interface MetadataResult {
        record Valid(PoolMetadata metadata) implements MetadataResult {}
        record Invalid(String reason) implements MetadataResult {}
        record TransientFailure(String reason) implements MetadataResult {}
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Fetch pool metadata from IPFS and validate the schema.
     *
     * @param cid the IPFS Content Identifier for the metadata JSON
     * @return a discriminated result: VALID, INVALID, or TRANSIENT_FAILURE
     */
    public MetadataResult fetchAndValidate(String cid) {
        if (cid == null || cid.isBlank()) {
            return new MetadataResult.Invalid("Empty metadata CID");
        }

        String url = properties.getIpfsGatewayUrl() + cid;
        log.debug("Fetching pool metadata from IPFS: {}", url);

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Network-level failure: timeout, connection refused, DNS failure
            // These are transient — the metadata might be perfectly valid.
            log.warn("Transient failure fetching metadata from IPFS for CID={}: {}", cid, e.getMessage());
            return new MetadataResult.TransientFailure("IPFS gateway unreachable: " + e.getMessage());
        }

        // HTTP-level responses
        int status = response.statusCode();
        if (status == 404) {
            // CID doesn't exist on IPFS — this is permanent, not transient
            return new MetadataResult.Invalid("IPFS returned 404 — CID does not exist: " + cid);
        }
        if (status == 429 || status >= 500) {
            // Rate limited or server error — transient
            log.warn("IPFS gateway returned {} for CID={} — transient failure", status, cid);
            return new MetadataResult.TransientFailure("IPFS gateway returned HTTP " + status);
        }
        if (status != 200) {
            // Other client errors (400, 403, etc.) — treat as permanent
            return new MetadataResult.Invalid("IPFS gateway returned HTTP " + status + " for CID=" + cid);
        }

        return parseAndValidate(response.body(), cid);
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private MetadataResult parseAndValidate(String json, String cid) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            // Malformed JSON — permanent, the CID will always return the same content
            return new MetadataResult.Invalid("Malformed JSON for CID=" + cid + ": " + e.getMessage());
        }

        // Validate required fields
        if (!root.has("title") || !root.has("description") ||
                !root.has("region") || !root.has("targetAmount")) {
            return new MetadataResult.Invalid(
                    "Missing required fields (title, description, region, targetAmount) for CID=" + cid);
        }

        String title = root.get("title").asText();
        String description = root.get("description").asText();
        String region = root.get("region").asText();
        long targetAmount = root.get("targetAmount").asLong();

        if (title.isBlank() || description.isBlank() || region.isBlank() || targetAmount <= 0) {
            return new MetadataResult.Invalid(
                    "Empty/invalid required fields for CID=" + cid);
        }

        // coverImage is optional
        String coverImage = root.has("coverImage") ? root.get("coverImage").asText(null) : null;

        return new MetadataResult.Valid(new PoolMetadata(title, description, region, coverImage, targetAmount));
    }
}
