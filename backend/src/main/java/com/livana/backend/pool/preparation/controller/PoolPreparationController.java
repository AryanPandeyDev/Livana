package com.livana.backend.pool.preparation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.livana.backend.common.exception.ApiException;
import com.livana.backend.pool.preparation.dto.CidResponse;
import com.livana.backend.pool.preparation.dto.PreparePoolRequest;
import com.livana.backend.pool.preparation.service.ImageValidator;
import com.livana.backend.pool.preparation.service.NgoAuthorizationService;
import com.livana.backend.pool.preparation.service.PinataClient;
import com.livana.backend.pool.preparation.service.PinataHeaders;
import com.livana.backend.pool.preparation.service.PoolMetadataJsonBuilder;
import com.livana.backend.pool.preparation.service.PoolMetadataValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/v1/pools")
@RequiredArgsConstructor
public class PoolPreparationController {

    private final NgoAuthorizationService ngoAuthorizationService;
    private final PinataClient pinataClient;
    private final PoolMetadataValidator metadataValidator;
    private final PoolMetadataJsonBuilder metadataJsonBuilder;

    /**
     * POST /api/v1/pools/upload-image
     * Multipart upload of cover image. Order: auth → multi-file check → image validation
     * → Pinata header extraction → Pinata upload. PinataHeaders are cleared in finally.
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CidResponse> uploadImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart(name = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        ngoAuthorizationService.requireApprovedNgo(jwt);

        // Reject more than one file part regardless of name (Requirement 1.4).
        // getMultiFileMap() returns ALL parts grouped by name (so two parts both
        // named "file" stay distinct), unlike getFileMap() which collapses to
        // one per name. We reject if any field has multiple parts OR if there
        // is more than one file field.
        if (request instanceof MultipartHttpServletRequest mr) {
            int totalFileParts = mr.getMultiFileMap()
                    .values()
                    .stream()
                    .mapToInt(java.util.List::size)
                    .sum();
            if (totalFileParts > 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                        "exactly one file part is allowed");
            }
        }

        ImageValidator.validate(file);
        PinataHeaders keys = PinataHeaders.fromRequest(request);
        try {
            String cid = pinataClient.pinFile(keys, file);
            return ResponseEntity.ok(new CidResponse(cid));
        } finally {
            keys.clear();
        }
    }

    /**
     * POST /api/v1/pools/prepare
     * JSON metadata upload. Order: auth → metadata validation → JSON build
     * → Pinata header extraction → Pinata upload. PinataHeaders cleared in finally.
     *
     * Body is bound as JsonNode so the validator can produce the same error codes
     * as IpfsMetadataService (binding to a record would coerce e.g. "100" to 100L).
     */
    @PostMapping(value = "/prepare", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CidResponse> prepare(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) JsonNode body,
            HttpServletRequest request) {
        ngoAuthorizationService.requireApprovedNgo(jwt);
        PreparePoolRequest validated = metadataValidator.validate(body);
        byte[] canonicalJson = metadataJsonBuilder.toCanonicalJson(validated);
        PinataHeaders keys = PinataHeaders.fromRequest(request);
        try {
            String cid = pinataClient.pinJson(keys, canonicalJson);
            return ResponseEntity.ok(new CidResponse(cid));
        } finally {
            keys.clear();
        }
    }
}
