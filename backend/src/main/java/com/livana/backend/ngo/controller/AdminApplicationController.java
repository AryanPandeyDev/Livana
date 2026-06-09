package com.livana.backend.ngo.controller;

import com.livana.backend.ngo.dto.AdminApplicationResponse;
import com.livana.backend.ngo.dto.AdminApprovalIntentRequest;
import com.livana.backend.ngo.dto.AdminRejectRequest;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.service.NgoApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-facing endpoints for reviewing NGO applications.
 * All endpoints require ADMIN role.
 *
 * From PRD:
 * - "Admin endpoints: list applications (filterable by status), view single
 *    application with AI results, mark approval intent"
 * - "NGO application data (all fields, AI screening results, documents) is
 *    restricted to ADMIN role only"
 */
@RestController
@RequestMapping("/api/v1/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final NgoApplicationService applicationService;

    /**
     * List all applications, optionally filtered by status.
     * Paginated, sorted by createdAt descending by default.
     */
    @GetMapping
    public ResponseEntity<Page<AdminApplicationResponse>> listApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AdminApplicationResponse> page = applicationService.listApplications(status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get a single application with full details including AI screening results.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminApplicationResponse> getApplication(@PathVariable UUID id) {
        AdminApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Record admin approval intent for an application in PENDING_REVIEW state.
     *
     * This does NOT approve the application on-chain. The actual approval happens
     * when 2-of-3 admins sign via the Safe multi-sig UI, and the backend reacts
     * to the resulting NGOApproved on-chain event.
     */
    @PostMapping("/{id}/approve-intent")
    public ResponseEntity<AdminApplicationResponse> approveIntent(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AdminApprovalIntentRequest request) {
        if (request == null) {
            request = new AdminApprovalIntentRequest(null);
        }
        AdminApplicationResponse response = applicationService.recordApprovalIntent(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject an application. Terminal state — the NGO can create a new application after this.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminApplicationResponse> rejectApplication(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRejectRequest request) {
        AdminApplicationResponse response = applicationService.rejectApplication(id, request);
        return ResponseEntity.ok(response);
    }
}
