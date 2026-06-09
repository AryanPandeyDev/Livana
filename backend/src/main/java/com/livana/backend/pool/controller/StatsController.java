package com.livana.backend.pool.controller;

import com.livana.backend.pool.dto.PlatformStatsDto;
import com.livana.backend.pool.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<PlatformStatsDto> getStats() {
        PlatformStatsDto stats = statsService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
}
