package com.livana.backend.ngo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the AI screening integration.
 *
 * Properties are under the ai-screening.* namespace.
 * Example:
 *   ai-screening.base-url=http://localhost:8000
 *   ai-screening.shared-secret=...
 *   ai-screening.callback-timeout-seconds=120
 */
@Configuration
@ConfigurationProperties(prefix = "ai-screening")
@Getter
@Setter
public class AiScreeningProperties {

    /**
     * Base URL of the AI_Screening_Service FastAPI app.
     * The Backend POSTs screening requests to {base-url}/screen.
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Mutual shared secret sent as the X-Internal-Secret header on both
     * directions of the integration (Backend→AI /screen and
     * AI→Backend /screening-callback). Never logged.
     */
    private String sharedSecret;

    /**
     * Window, in seconds, after which an application still stuck in
     * AI_SCREENING is swept to PENDING_REVIEW by the scheduled timeout job.
     */
    private long callbackTimeoutSeconds = 120;
}
