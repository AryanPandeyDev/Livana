package com.livana.backend.ngo.service;

import com.livana.backend.auth.entity.User;
import com.livana.backend.auth.service.UserService;
import com.livana.backend.common.exception.ApiException;
import com.livana.backend.ngo.dto.*;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.entity.NgoApplication;
import com.livana.backend.ngo.repository.NgoApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enforces the NGO application status machine:
 *   DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED / REJECTED
 *
 * Email verification is delegated to Clerk:
 * - The officialEmail does NOT have to equal the user's primary Clerk login email.
 * - The frontend guides the applicant to add officialEmail to their Clerk account
 *   and verify it through Clerk's OTP/magic-link flow.
 * - On submit, the backend calls the Clerk Backend API to confirm the officialEmail
 *   is a verified email on the authenticated Clerk user's account.
 * - The backend does NOT generate OTPs or send verification emails.
 *
 * Role model:
 * - Clerk verifies: applicant controls this email inbox.
 * - AI screening verifies: email/org/documents look legitimate and associated.
 * - Admin verifies: evidence is strong enough to whitelist on-chain.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NgoApplicationService {

    private static final List<ApplicationStatus> TERMINAL_STATUSES =
            List.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED);

    private final NgoApplicationRepository applicationRepository;
    private final UserService userService;
    private final ClerkEmailVerificationService clerkEmailVerificationService;
    private final AiScreeningService aiScreeningService;

    // ========================================================================
    // NGO-facing operations
    // ========================================================================

    /**
     * Create a new application in DRAFT state.
     * Requires authenticated user with linked wallet.
     *
     * The officialEmail is stored as-is — no verification check at this stage.
     * The applicant will add/verify this email through Clerk before submitting.
     *
     * DB partial unique index enforces one active application per user.
     */
    @Transactional
    public ApplicationResponse createApplication(String clerkId, CreateApplicationRequest request) {
        User user = userService.getAuthenticatedUserWithWallet(clerkId);

        NgoApplication application = NgoApplication.builder()
                .user(user)
                .orgName(request.orgName())
                .registrationNumber(request.registrationNumber())
                .description(request.description())
                .officialEmail(request.officialEmail())
                .documentsCid(request.documentsCid())
                .walletAddress(user.getWalletAddress())
                .status(ApplicationStatus.DRAFT)
                .build();

        try {
            application = applicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            // Partial unique index: uq_ngo_applications_active_user
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_ALREADY_EXISTS",
                    "You already have an active application. Complete or withdraw it before creating a new one.");
        }

        log.info("Created NGO application id={} for user clerkId={}", application.getId(), clerkId);
        return toApplicationResponse(application);
    }

    /**
     * Submit a DRAFT application → transitions to AI_SCREENING.
     *
     * Before advancing, the backend calls the Clerk Backend API to confirm
     * that officialEmail is a verified email on this Clerk user's account.
     * The frontend should have guided the user to add/verify it through Clerk first.
     *
     * If Clerk confirms the email is verified: DRAFT → AI_SCREENING.
     * If not: returns EMAIL_NOT_VERIFIED so the frontend can prompt verification.
     */
    @Transactional
    public ApplicationResponse submitApplication(String clerkId) {
        User user = userService.getAuthenticatedUserWithWallet(clerkId);
        NgoApplication application = getActiveApplication(user.getId());

        assertStatus(application, ApplicationStatus.DRAFT, "submit");

        // Verify officialEmail is a verified email on this Clerk user account.
        // This call hits the Clerk Backend API — fails closed on any error.
        boolean emailVerified = clerkEmailVerificationService.isVerifiedEmailForUser(
                user.getClerkId(), application.getOfficialEmail());

        if (!emailVerified) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED",
                    "The official email (" + application.getOfficialEmail() + ") is not yet verified " +
                    "on your Clerk account. Please add and verify it through the email verification flow first.");
        }

        // Email verified by Clerk → transition to AI_SCREENING
        application.setStatus(ApplicationStatus.AI_SCREENING);
        application = applicationRepository.save(application);
        log.info("Application id={} submitted → AI_SCREENING (officialEmail={} verified by Clerk)",
                application.getId(), application.getOfficialEmail());

        // Fire-and-forget async screening. The application stays in AI_SCREENING until
        // the AI service calls back into completeAiScreening(...), or a fallback path
        // (trigger failure / timeout sweep) advances it to PENDING_REVIEW.
        aiScreeningService.triggerScreening(toScreeningRequest(application));

        return toApplicationResponse(application);
    }

    /**
     * Get the current user's most recent application, including terminal states.
     * This allows NGOs to see their APPROVED or REJECTED status after the
     * application process completes.
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getMyApplication(String clerkId) {
        User user = userService.getAuthenticatedUserWithWallet(clerkId);
        NgoApplication application = applicationRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_APPLICATION_FOUND",
                        "No application found. Create one first."));
        return toApplicationResponse(application);
    }

    // ========================================================================
    // AI screening transitions (called by the async trigger, the callback
    // controller, and the timeout sweep). All transitions out of AI_SCREENING
    // use an atomic conditional update so exactly one writer wins the race.
    // ========================================================================

    /** Callback success: store results and advance, only if still AI_SCREENING. Returns true if this call won. */
    @Transactional
    public boolean completeAiScreening(UUID appId, BigDecimal score, String summary, String verdict) {
        int updated = applicationRepository.completeScreening(
                appId, ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW, score, summary, verdict);
        return updated == 1;
    }

    /** Fallback (trigger failure, key failure, timeout): advance with null results, only if still AI_SCREENING. */
    @Transactional
    public boolean fallbackToManualReview(UUID appId) {
        int updated = applicationRepository.fallbackToPendingReview(
                appId, ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW);
        return updated == 1;
    }

    /** Used by the callback controller to distinguish unknown application (404) from already-advanced (200 no-op). */
    @Transactional(readOnly = true)
    public boolean applicationExists(UUID appId) {
        return applicationRepository.existsById(appId);
    }

    // ========================================================================
    // Admin operations
    // ========================================================================

    /**
     * Admin: list all applications, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<AdminApplicationResponse> listApplications(ApplicationStatus status, Pageable pageable) {
        Page<NgoApplication> page;
        if (status != null) {
            page = applicationRepository.findByStatus(status, pageable);
        } else {
            page = applicationRepository.findAllBy(pageable);
        }
        return page.map(this::toAdminResponse);
    }

    /**
     * Admin: get a single application by ID with full details including AI results.
     */
    @Transactional(readOnly = true)
    public AdminApplicationResponse getApplicationById(UUID applicationId) {
        NgoApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                        "Application not found"));
        return toAdminResponse(application);
    }

    /**
     * Admin: record approval intent for an application in PENDING_REVIEW.
     *
     * This does NOT approve the application on-chain. The actual on-chain
     * whitelisting (addVerifiedNGO) happens via the Safe multi-sig UI.
     * The backend will react to the NGOApproved on-chain event to complete
     * the approval (handled by the Event Indexer module).
     */
    @Transactional
    public AdminApplicationResponse recordApprovalIntent(UUID applicationId, AdminApprovalIntentRequest request) {
        NgoApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                        "Application not found"));

        assertStatus(application, ApplicationStatus.PENDING_REVIEW, "approve");

        // Always append a timestamped approval intent record to adminNotes
        // so the backend has a traceable audit trail even without a dedicated DB column.
        String intentRecord = "[APPROVAL_INTENT " + OffsetDateTime.now() + "]";
        if (request.adminNotes() != null) {
            intentRecord += " " + request.adminNotes();
        }
        String existing = application.getAdminNotes();
        application.setAdminNotes(existing == null ? intentRecord : existing + "\n" + intentRecord);

        // Status stays at PENDING_REVIEW — it transitions to APPROVED only when
        // the on-chain NGOApproved event is indexed for this wallet address.
        application = applicationRepository.save(application);

        log.info("Admin recorded approval intent for application id={}", applicationId);
        return toAdminResponse(application);
    }

    /**
     * Admin: reject an application. Terminal state — no further transitions.
     */
    @Transactional
    public AdminApplicationResponse rejectApplication(UUID applicationId, AdminRejectRequest request) {
        NgoApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                        "Application not found"));

        assertStatus(application, ApplicationStatus.PENDING_REVIEW, "reject");

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(request.rejectionReason());
        if (request.adminNotes() != null) {
            application.setAdminNotes(request.adminNotes());
        }

        application = applicationRepository.save(application);

        log.info("Application id={} rejected by admin", applicationId);
        return toAdminResponse(application);
    }

    // ========================================================================
    // Event-driven transitions (called by Event Indexer — future module)
    // ========================================================================

    /**
     * Called when the Event Indexer processes an NGOApproved event.
     * Finds the matching application by wallet address and transitions it to APPROVED.
     * Also promotes the user's role from USER to NGO.
     *
     * From PRD: "When NGOApproved event is indexed for a matching wallet address,
     * the application status transitions to APPROVED automatically."
     *
     * Design: Only matches applications in PENDING_REVIEW. The expected flow is
     * that admins review the application (AI results, documents) in the backend
     * dashboard first, record approval intent, then sign via Safe multi-sig.
     * If an on-chain NGOApproved event fires for a wallet whose application has
     * not yet reached PENDING_REVIEW, the event is indexed in indexed_events but
     * does NOT auto-approve the application — the backend workflow must complete first.
     */
    @Transactional
    public void onNgoApprovedEvent(String walletAddress) {
        String normalizedAddress = walletAddress.toLowerCase();

        // Only match PENDING_REVIEW — on-chain approval must not bypass the
        // backend vetting workflow (AI screening → admin review).
        applicationRepository.findByWalletAddressAndStatus(normalizedAddress, ApplicationStatus.PENDING_REVIEW)
                .ifPresent(application -> {
                    application.setStatus(ApplicationStatus.APPROVED);
                    applicationRepository.save(application);

                    // Promote user role to NGO
                    User user = application.getUser();
                    user.setRole("NGO");

                    log.info("Application id={} auto-approved via on-chain NGOApproved event for wallet={}",
                            application.getId(), normalizedAddress);
                });
    }

    // ========================================================================
    // Private helpers
    // ========================================================================


    private NgoApplication getActiveApplication(UUID userId) {
        return applicationRepository.findByUserIdAndStatusNotIn(userId, TERMINAL_STATUSES)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_ACTIVE_APPLICATION",
                        "No active application found. Create one first."));
    }

    private void assertStatus(NgoApplication application, ApplicationStatus expected, String action) {
        if (application.getStatus() != expected) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    String.format("Cannot %s: application is in %s state, expected %s",
                            action, application.getStatus(), expected));
        }
    }

    private ScreeningRequest toScreeningRequest(NgoApplication app) {
        return new ScreeningRequest(
                app.getId(), app.getOrgName(), app.getRegistrationNumber(),
                app.getDescription(), app.getOfficialEmail(), app.getDocumentsCid(),
                null /* geminiApiKey injected by the trigger */);
    }

    private ApplicationResponse toApplicationResponse(NgoApplication app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .orgName(app.getOrgName())
                .registrationNumber(app.getRegistrationNumber())
                .description(app.getDescription())
                .officialEmail(app.getOfficialEmail())
                .documentsCid(app.getDocumentsCid())
                .walletAddress(app.getWalletAddress())
                .status(app.getStatus().name())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private AdminApplicationResponse toAdminResponse(NgoApplication app) {
        return AdminApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser().getId())
                .userEmail(app.getUser().getEmail())
                .orgName(app.getOrgName())
                .registrationNumber(app.getRegistrationNumber())
                .description(app.getDescription())
                .officialEmail(app.getOfficialEmail())
                .documentsCid(app.getDocumentsCid())
                .walletAddress(app.getWalletAddress())
                .status(app.getStatus().name())
                .aiConfidenceScore(app.getAiConfidenceScore())
                .aiResearchSummary(app.getAiResearchSummary())
                .aiVerdict(app.getAiVerdict())
                .adminNotes(app.getAdminNotes())
                .rejectionReason(app.getRejectionReason())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
