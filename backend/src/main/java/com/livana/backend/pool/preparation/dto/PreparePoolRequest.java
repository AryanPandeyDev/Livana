package com.livana.backend.pool.preparation.dto;

/**
 * Validated pool metadata produced by {@code PoolMetadataValidator}.
 * <p>
 * No Bean Validation annotations: validation rules live in
 * {@code PoolMetadataValidator} so error codes match
 * {@code IpfsMetadataService} semantics exactly.
 * <p>
 * {@code coverImage} is nullable.
 */
public record PreparePoolRequest(
        String title,
        String description,
        String region,
        String coverImage,
        long targetAmount
) {}
