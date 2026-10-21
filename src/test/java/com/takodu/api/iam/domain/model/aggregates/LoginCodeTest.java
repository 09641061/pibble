package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginCodeTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    private final LoginCode code = LoginCode.issue(new Email("ana@example.com"), SecretHash.of("123456"), NOW);

    @Test
    void newCodeIsUsableForTenMinutes() {
        assertTrue(code.isUsable(NOW.plusSeconds(599)));
        assertFalse(code.isUsable(NOW.plusSeconds(600)));
    }

    @Test
    void codeMatchesOnlyItsOwnDigits() {
        assertTrue(code.matches(SecretHash.of("123456")));
        assertFalse(code.matches(SecretHash.of("654321")));
    }

    @Test
    void fiveWrongAttemptsExhaustTheCode() {
        for (int i = 0; i < 4; i++) code.recordFailedAttempt();
        assertTrue(code.isUsable(NOW));

        code.recordFailedAttempt();

        assertFalse(code.isUsable(NOW));
    }

    @Test
    void consumedCodeIsNotUsable() {
        code.consume(NOW);

        assertFalse(code.isUsable(NOW));
    }
}
