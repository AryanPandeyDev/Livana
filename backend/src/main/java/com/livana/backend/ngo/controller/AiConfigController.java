package com.livana.backend.ngo.controller;

import com.livana.backend.ngo.dto.AiConfigStatusResponse;
import com.livana.backend.ngo.dto.SetAiConfigRequest;
import com.livana.backend.ngo.service.AiConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only management of the Gemini API key used by AI screening.
 * The raw key is never returned — GET returns a masked form + provenance.
 */
@RestController
@RequestMapping("/api/v1/admin/ai-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @PostMapping
    public ResponseEntity<AiConfigStatusResponse> setKey(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetAiConfigRequest request) {
        aiConfigService.setKey(request.geminiApiKey(), jwt.getSubject());
        return ResponseEntity.ok(aiConfigService.getStatus());
    }

    @GetMapping
    public ResponseEntity<AiConfigStatusResponse> getStatus() {
        return ResponseEntity.ok(aiConfigService.getStatus());
    }
}
