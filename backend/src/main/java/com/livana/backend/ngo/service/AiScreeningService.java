package com.livana.backend.ngo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livana.backend.ngo.config.AiScreeningProperties;
import com.livana.backend.ngo.dto.ScreeningRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Triggers asynchronous AI screening by POSTing to the external AI service /screen.
 * Fail-open: any delivery failure (no key, connect error, timeout, non-2xx) falls the
 * application back to manual review (PENDING_REVIEW) so a flaky AI never blocks an NGO.
 *
 * The decrypted Gemini key is forwarded in the request body and is NEVER logged.
 */
@Service
@Slf4j
public class AiScreeningService {

    private final AiConfigService aiConfigService;
    private final NgoApplicationService ngoApplicationService;
    private final AiScreeningProperties properties;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Explicit constructor (not {@code @RequiredArgsConstructor}) so {@code @Lazy} can be applied
     * to the {@link NgoApplicationService} parameter. AiScreeningService and NgoApplicationService
     * depend on each other; even though {@link #triggerScreening} is {@code @Async}, Spring still
     * has to construct both beans, and constructor injection would otherwise fail with a circular
     * reference error at startup. The lazy proxy breaks that cycle.
     */
    public AiScreeningService(AiConfigService aiConfigService,
                              @Lazy NgoApplicationService ngoApplicationService,
                              AiScreeningProperties properties,
                              ObjectMapper objectMapper) {
        this.aiConfigService = aiConfigService;
        this.ngoApplicationService = ngoApplicationService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Async
    public void triggerScreening(ScreeningRequest request) {
        String geminiKey;
        try {
            geminiKey = aiConfigService.getDecryptedKey()
                    .orElseThrow(() -> new IllegalStateException("No Gemini key configured"));
        } catch (Exception e) {
            log.warn("Gemini key unavailable for app {} — falling back to manual review",
                    request.applicationId());
            ngoApplicationService.fallbackToManualReview(request.applicationId());
            return;
        }

        try {
            String body = objectMapper.writeValueAsString(request.withGeminiKey(geminiKey));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/screen"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", properties.getSharedSecret())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("AI /screen returned {} for app {} — fallback",
                        resp.statusCode(), request.applicationId());
                ngoApplicationService.fallbackToManualReview(request.applicationId());
            } else {
                log.info("AI screening triggered for app {} (status {})",
                        request.applicationId(), resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("AI /screen delivery failed for app {} — fallback: {}",
                    request.applicationId(), e.getMessage());
            ngoApplicationService.fallbackToManualReview(request.applicationId());
        }
    }
}
