package com.livana.backend.common.config;

import com.livana.backend.ngo.config.AiScreeningProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class InternalApiSecurityConfig {

    private final AiScreeningProperties properties;

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/internal/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(new InternalSecretFilter(properties.getSharedSecret()),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
