package com.livana.backend.pool.controller;

import com.livana.backend.common.validation.AddressValidator;
import com.livana.backend.pool.dto.PoolDetailDto;
import com.livana.backend.pool.dto.PoolSummaryDto;
import com.livana.backend.pool.service.PoolQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pools")
@RequiredArgsConstructor
public class PoolController {

    private final PoolQueryService poolQueryService;

    @GetMapping
    public ResponseEntity<Page<PoolSummaryDto>> listPools(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PoolSummaryDto> pools = poolQueryService.listPools(region, search, pageable);
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/{address}")
    public ResponseEntity<PoolDetailDto> getPool(@PathVariable String address) {
        String normalized = AddressValidator.validateAndNormalize(address, "address");
        PoolDetailDto pool = poolQueryService.getPool(normalized);
        return ResponseEntity.ok(pool);
    }
}
