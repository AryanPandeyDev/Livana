package com.livana.backend.indexer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Creates the Web3j bean for blockchain interaction.
 *
 * Uses HTTP transport (not WebSocket) for reliability and simplicity.
 * HTTP-based eth_getLogs polling works well for the ~2s block times on
 * Avalanche C-Chain and eliminates WebSocket reconnection complexity.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class Web3jConfig {

    private final IndexerProperties properties;

    @Bean
    public Web3j web3j() {
        String rpcUrl = properties.getRpcUrl();
        log.info("Connecting to blockchain RPC: {}", rpcUrl);
        return Web3j.build(new HttpService(rpcUrl));
    }
}
