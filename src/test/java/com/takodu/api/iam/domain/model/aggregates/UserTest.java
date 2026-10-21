package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.exceptions.UserDisabledException;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Test
    void signInRecordsTheInstant() {
        var user = User.register(new Email("ana@example.com"));

        user.signIn(NOW);

        assertEquals(NOW, user.getLastLoginAt());
    }

    @Test
    void disabledUserCannotSignIn() {
        var user = new User(new UserId(UUID.randomUUID()), new Email("ana@example.com"), null, NOW.minusSeconds(60), null);

        assertThrows(UserDisabledException.class, () -> user.signIn(NOW));
        assertNull(user.getLastLoginAt());
    }
}
