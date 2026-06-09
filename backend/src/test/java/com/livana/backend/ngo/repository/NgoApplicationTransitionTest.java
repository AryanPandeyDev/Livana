package com.livana.backend.ngo.repository;

import com.livana.backend.auth.entity.User;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.entity.NgoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the conditional state transitions that guarantee AI-screening
 * advancement is exactly-once and idempotent.
 *
 * The callback, the trigger-failure fallback, and the timeout sweep all race to
 * move an application out of AI_SCREENING. Correctness rests entirely on the
 * `UPDATE ... WHERE status = 'AI_SCREENING'` clause and its rows-affected count:
 * whichever runs first wins; everyone else is a no-op. This is the highest-risk
 * logic in the AI-screening feature, so it gets direct coverage. H2 in PostgreSQL mode.
 *
 * Production behaviors protected:
 * - completeScreening writes results + advances only from AI_SCREENING (returns 1),
 *   and is a no-op (returns 0) from any other status.
 * - fallbackToPendingReview advances only from AI_SCREENING, leaving AI fields null.
 * - a second transition attempt after the first wins returns 0 and changes nothing
 *   (idempotency / exactly-once).
 * - findStaleScreeningIds only returns AI_SCREENING rows older than the threshold.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NgoApplicationTransitionTest {

    @Autowired
    private NgoApplicationRepository applicationRepository;

    @Autowired
    private TestEntityManager em;

    private int seq = 0;

    private User persistUser() {
        seq++;
        User user = User.builder()
                .clerkId("clerk_" + seq)
                .email("user" + seq + "@example.com")
                .walletAddress(String.format("0x%040d", seq))
                .role("USER")
                .build();
        return em.persistAndFlush(user);
    }

    private NgoApplication persistApplication(ApplicationStatus status) {
        User user = persistUser();
        NgoApplication app = NgoApplication.builder()
                .user(user)
                .orgName("Org " + seq)
                .registrationNumber("REG-" + seq)
                .description("desc")
                .officialEmail("org" + seq + "@example.com")
                .walletAddress(user.getWalletAddress())
                .status(status)
                .build();
        return em.persistAndFlush(app);
    }

    private NgoApplication reload(UUID id) {
        em.clear();
        return applicationRepository.findById(id).orElseThrow();
    }

    // ========================================================================
    // completeScreening
    // ========================================================================

    @Test
    @DisplayName("completeScreening advances from AI_SCREENING and stores AI results")
    void completeScreeningFromAiScreening() {
        NgoApplication app = persistApplication(ApplicationStatus.AI_SCREENING);

        int updated = applicationRepository.completeScreening(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW,
                new BigDecimal("87.50"), "looks legit", "PASS");

        assertThat(updated).isEqualTo(1);

        NgoApplication reloaded = reload(app.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(reloaded.getAiConfidenceScore()).isEqualByComparingTo("87.50");
        assertThat(reloaded.getAiResearchSummary()).isEqualTo("looks legit");
        assertThat(reloaded.getAiVerdict()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("completeScreening is a no-op when the application is not in AI_SCREENING")
    void completeScreeningNoOpFromWrongStatus() {
        NgoApplication app = persistApplication(ApplicationStatus.PENDING_REVIEW);

        int updated = applicationRepository.completeScreening(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW,
                new BigDecimal("99.99"), "should not apply", "PASS");

        assertThat(updated).isZero();

        NgoApplication reloaded = reload(app.getId());
        // Status unchanged and no AI fields written.
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(reloaded.getAiVerdict()).isNull();
        assertThat(reloaded.getAiConfidenceScore()).isNull();
    }

    // ========================================================================
    // fallbackToPendingReview
    // ========================================================================

    @Test
    @DisplayName("fallbackToPendingReview advances from AI_SCREENING leaving AI fields null")
    void fallbackAdvancesWithNullResults() {
        NgoApplication app = persistApplication(ApplicationStatus.AI_SCREENING);

        int updated = applicationRepository.fallbackToPendingReview(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW);

        assertThat(updated).isEqualTo(1);

        NgoApplication reloaded = reload(app.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(reloaded.getAiConfidenceScore()).isNull();
        assertThat(reloaded.getAiResearchSummary()).isNull();
        assertThat(reloaded.getAiVerdict()).isNull();
    }

    // ========================================================================
    // Exactly-once / idempotency under racing transitions
    // ========================================================================

    @Test
    @DisplayName("only the first transition wins; the second is a no-op (callback then sweep)")
    void exactlyOnceCallbackThenSweep() {
        NgoApplication app = persistApplication(ApplicationStatus.AI_SCREENING);

        // Callback wins first.
        int first = applicationRepository.completeScreening(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW,
                new BigDecimal("70.00"), "ai summary", "PASS");
        // Timeout sweep arrives late.
        int second = applicationRepository.fallbackToPendingReview(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero(); // sweep finds nothing in AI_SCREENING

        // The AI results from the winning callback survive — not wiped by the sweep.
        NgoApplication reloaded = reload(app.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(reloaded.getAiVerdict()).isEqualTo("PASS");
        assertThat(reloaded.getAiResearchSummary()).isEqualTo("ai summary");
    }

    @Test
    @DisplayName("when the sweep wins first, a late callback does not overwrite the result fields")
    void exactlyOnceSweepThenCallback() {
        NgoApplication app = persistApplication(ApplicationStatus.AI_SCREENING);

        // Sweep wins first → PENDING_REVIEW with null results.
        int first = applicationRepository.fallbackToPendingReview(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW);
        // Late callback tries to write results.
        int second = applicationRepository.completeScreening(
                app.getId(), ApplicationStatus.AI_SCREENING, ApplicationStatus.PENDING_REVIEW,
                new BigDecimal("88.00"), "late summary", "PASS");

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero(); // callback finds nothing in AI_SCREENING

        NgoApplication reloaded = reload(app.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        // Results stay null — the late callback was correctly ignored.
        assertThat(reloaded.getAiVerdict()).isNull();
        assertThat(reloaded.getAiConfidenceScore()).isNull();
    }

    // ========================================================================
    // findStaleScreeningIds (timeout sweep query)
    // ========================================================================

    @Test
    @DisplayName("findStaleScreeningIds returns only AI_SCREENING rows older than the threshold")
    void findStaleScreeningIdsFiltersByStatusAndAge() {
        NgoApplication stale = persistApplication(ApplicationStatus.AI_SCREENING);
        NgoApplication pending = persistApplication(ApplicationStatus.PENDING_REVIEW);

        // Threshold in the future so the just-created AI_SCREENING row counts as stale,
        // while the PENDING_REVIEW row is excluded by status regardless of age.
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC)
                .plus(1, ChronoUnit.HOURS);

        List<UUID> staleIds = applicationRepository.findStaleScreeningIds(
                ApplicationStatus.AI_SCREENING, threshold);

        assertThat(staleIds).contains(stale.getId());
        assertThat(staleIds).doesNotContain(pending.getId());
    }

    @Test
    @DisplayName("findStaleScreeningIds excludes rows newer than the threshold")
    void findStaleScreeningIdsExcludesFresh() {
        persistApplication(ApplicationStatus.AI_SCREENING);

        // Threshold in the past → nothing is old enough to be swept.
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC)
                .minus(1, ChronoUnit.HOURS);

        List<UUID> staleIds = applicationRepository.findStaleScreeningIds(
                ApplicationStatus.AI_SCREENING, threshold);

        assertThat(staleIds).isEmpty();
    }
}
