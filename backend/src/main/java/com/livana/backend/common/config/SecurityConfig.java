package com.livana.backend.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
}
