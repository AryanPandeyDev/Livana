package com.livana.backend.pool.preparation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livana.backend.pool.preparation.dto.PreparePoolRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the canonical metadata JSON byte stream that gets pinned to IPFS.
 *
 * <p>Field order is fixed: {@code title}, {@code description}, {@code region},
 * {@code coverImage} (optional), {@code targetAmount}. The indexer reads fields
 * by name so order is irrelevant for round-trip, but stable order keeps test
 * assertions and logs predictable.
 *
 * <p>{@code targetAmount} uses {@link ObjectNode#put(String, long)} which
 * serializes as a JSON integer literal (no quotes, no decimal) — matching
 * {@code IpfsMetadataService.parseAndValidate}'s {@code JsonNode.asLong()} reader.
 */
@Component
@RequiredArgsConstructor
public class PoolMetadataJsonBuilder {

    private final ObjectMapper objectMapper;

    public byte[] toCanonicalJson(PreparePoolRequest req) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", req.title());
        root.put("description", req.description());
        root.put("region", req.region());
        if (req.coverImage() != null) {
            root.put("coverImage", req.coverImage());
        }
        root.put("targetAmount", req.targetAmount());
        try {
            return objectMapper.writeValueAsBytes(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unreachable: ObjectNode is always serializable", e);
        }
    }
}
