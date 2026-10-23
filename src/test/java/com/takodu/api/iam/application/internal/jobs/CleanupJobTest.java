package com.takodu.api.iam.application.internal.jobs;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import com.takodu.api.shared.infrastructure.time.TestClock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class CleanupJobTest {

    @Inject
    CleanupJob cleanupJob;

    @Inject
    RecordingEmailSender emailSender;

    @Inject
    TestClock clock;

    @Inject
    EntityManager entityManager;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long rows(String table, String email) {
        var sql = switch (table) {
            case "login_codes" -> "select count(*) from login_codes where email = :email";
            default -> "select count(*) from sessions s join users u on u.id = s.user_id where u.email = :email";
        };
        return ((Number) entityManager.createNativeQuery(sql).setParameter("email", email).getSingleResult()).longValue();
    }

    @Test
    void deletesCodesAndSessionsOneDayAfterTheyEnd() {
        var email = SignIn.newEmail();
        clock.set(Instant.now());
        SignIn.as(email, emailSender);

        clock.advance(Duration.ofDays(30).plus(CleanupJob.RETENTION).plusSeconds(1));
        cleanupJob.deleteEnded();

        assertEquals(0, rows("login_codes", email));
        assertEquals(0, rows("sessions", email));
    }

    @Test
    void keepsWhatEndedLessThanADayAgo() {
        var email = SignIn.newEmail();
        clock.set(Instant.now());
        SignIn.as(email, emailSender);

        clock.advance(Duration.ofHours(23));
        cleanupJob.deleteEnded();

        assertEquals(1, rows("login_codes", email));
        assertEquals(1, rows("sessions", email));
    }
}
