package com.livana.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a Jackson 2 (com.fasterxml) ObjectMapper bean.
 *
 * Spring Boot 4.x auto-configures a Jackson 3 (tools.jackson) ObjectMapper,
 * but web3j and our indexer code use Jackson 2. This bean provides a Jackson 2
 * ObjectMapper for injection where com.fasterxml.jackson.databind.ObjectMapper
 * is expected.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper fasterxmlObjectMapper() {
        return new ObjectMapper();
    }
}
