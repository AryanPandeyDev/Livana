package com.livana.backend.pool.preparation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Exposes the {@link HttpClient} used by {@code PinataClient} as a Spring bean
 * so tests can swap in a fake. Same seam used by {@code IpfsMetadataService}.
 */
@Configuration
public class PinataHttpConfig {

    @Bean
    public HttpClient pinataHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
}
