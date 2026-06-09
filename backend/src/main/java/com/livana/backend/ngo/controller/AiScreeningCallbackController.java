package com.livana.backend.ngo.controller;

import com.livana.backend.ngo.dto.ScreeningCallbackRequest;
import com.livana.backend.ngo.service.NgoApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal machine-to-machine callback for AI screening results.
 * Authentication is enforced by InternalSecretFilter (shared secret) on the
 * /api/v1/internal/** chain — NOT by Clerk JWT.
 *
 * Idempotent: completeAiScreening only writes if the application is still in
 * AI_SCREENING, so a duplicate callback (or one racing the timeout sweep) is a
 * safe no-op that still returns 200.
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Slf4j
public class AiScreeningCallbackController {

    private final NgoApplicationService ngoApplicationService;

    @PostMapping("/screening-callback")
    public ResponseEntity<Void> handleCallback(@Valid @RequestBody ScreeningCallbackRequest body) {
        if (!ngoApplicationService.applicationExists(body.applicationId())) {
            return ResponseEntity.notFound().build(); // 404, no mutation
        }
        boolean won = ngoApplicationService.completeAiScreening(
                body.applicationId(), body.confidenceScore(), body.researchSummary(), body.verdict());
        if (won) {
            log.info("Stored AI screening result for app {} (verdict {})",
                    body.applicationId(), body.verdict());
        } else {
            log.info("Callback for app {} ignored — already advanced (idempotent no-op)",
                    body.applicationId());
        }
        return ResponseEntity.ok().build(); // 200 whether we won or it was already advanced
    }
}
