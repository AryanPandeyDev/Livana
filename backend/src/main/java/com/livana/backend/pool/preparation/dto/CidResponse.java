package com.livana.backend.pool.preparation.dto;

/**
 * Stable success response for both {@code /upload-image} and {@code /prepare}.
 * Single field {@code cid} so the frontend has one parsing path.
 */
public record CidResponse(String cid) {}
