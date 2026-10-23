package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.exceptions.InvalidRefreshTokenException;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");
    private static final SecretHash FIRST = SecretHash.of("first");
    private static final SecretHash SECOND = SecretHash.of("second");
    private static final SecretHash THIRD = SecretHash.of("third");

    private final Session session = Session.open(new UserId(UUID.randomUUID()), FIRST, NOW);

    @Test
    void refreshRotatesTheTokenAndExtendsTheSession() {
        var later = NOW.plus(Duration.ofDays(1));

        session.refresh(FIRST, SECOND, later);

        assertEquals(SECOND, session.getRefreshTokenHash());
        assertEquals(later.plus(Session.TTL), session.getExpiresAt());
    }

    @Test
    void previousTokenRetriedWithinTwoMinutesRotatesAgain() {
        session.refresh(FIRST, SECOND, NOW);

        session.refresh(FIRST, THIRD, NOW.plus(Session.RETRY_WINDOW));

        assertEquals(THIRD, session.getRefreshTokenHash());
        assertTrue(session.isActive(NOW.plus(Session.RETRY_WINDOW)));
    }

    @Test
    void previousTokenReusedLaterRevokesTheSession() {
        session.refresh(FIRST, SECOND, NOW);
        var late = NOW.plus(Session.RETRY_WINDOW).plusSeconds(1);

        assertThrows(InvalidRefreshTokenException.class, () -> session.refresh(FIRST, THIRD, late));
        assertFalse(session.isActive(late));
    }

    @Test
    void retriesDoNotExtendTheRetryWindow() {
        session.refresh(FIRST, SECOND, NOW);
        session.refresh(FIRST, THIRD, NOW.plusSeconds(100));

        assertThrows(InvalidRefreshTokenException.class,
                () -> session.refresh(FIRST, SecretHash.of("fourth"), NOW.plusSeconds(200)));
    }

    @Test
    void unknownTokenIsRefusedWithoutRevoking() {
        assertThrows(InvalidRefreshTokenException.class, () -> session.refresh(THIRD, SECOND, NOW));
        assertTrue(session.isActive(NOW));
    }

    @Test
    void expiredSessionCannotBeRefreshed() {
        assertThrows(InvalidRefreshTokenException.class, () -> session.refresh(FIRST, SECOND, NOW.plus(Session.TTL)));
    }

    @Test
    void revokedSessionCannotBeRefreshed() {
        session.revoke(NOW);

        assertThrows(InvalidRefreshTokenException.class, () -> session.refresh(FIRST, SECOND, NOW));
    }
}
