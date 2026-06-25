package com.livana.backend.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health check
                .requestMatchers("/actuator/health").permitAll()
                // Public endpoints (pools browsing, leaderboards, stats)
                .requestMatchers(HttpMethod.GET, "/api/v1/pools/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/donations/leaderboard").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/donations/pool/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/reputation/**").permitAll()
                // /proofs/me is authenticated (self-scoped NGO view) — must come
                // before the public /proofs/** wildcard since matchers are ordered.
                .requestMatchers(HttpMethod.GET, "/api/v1/proofs/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/proofs/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );

        return http.build();
    }

    /**
     * Custom Clerk JWT decoder.
     *
     * The auto-configured decoder re-fetched Clerk's JWKS (`/.well-known/jwks.json`) on EVERY
     * authenticated request (the JWK source resolved to a NoOpCache) using an aggressive default
     * read timeout — which produced intermittent "Read timed out" failures and 500s on every
     * authenticated endpoint. This decoder:
     *   - caches the JWK set (fetched once, reused; refreshed on an unknown key id), and
     *   - uses generous 10s connect/read timeouts so an occasionally-cold fetch doesn't trip.
     * Issuer validation is preserved when an issuer-uri is configured.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestTemplate restOperations = new RestTemplate(requestFactory);

        Cache jwkSetCache = new ConcurrentMapCache("clerk-jwk-set");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(restOperations)
                .cache(jwkSetCache)
                .build();

        if (!issuerUri.isBlank()) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        }
        return decoder;
    }
}
