package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * I2: a 6-digit code emailed to sign in, valid 10 minutes, used once, refused after 5 wrong attempts. Only its
 * hash is stored.
 */
@Getter
public class LoginCode {
    public static final Duration TTL = Duration.ofMinutes(10);
    public static final int MAX_ATTEMPTS = 5;
    /** I3: codes one email may request per {@link #RATE_WINDOW}. */
    public static final int MAX_PER_WINDOW = 5;
    public static final Duration RATE_WINDOW = Duration.ofHours(1);

    private final LoginCodeId id;
    private final Email email;
    private final SecretHash codeHash;
    private int attempts;
    private final Instant expiresAt;
    private Instant consumedAt;

    /**
     * @param email where the code is sent
     * @param codeHash hash of the plain code, which only the email carries
     * @param now the current instant
     * @return a new unused code expiring after {@link #TTL}
     */
    public static LoginCode issue(Email email, SecretHash codeHash, Instant now) {
        return new LoginCode(null, email, codeHash, 0, now.plus(TTL), null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public LoginCode(LoginCodeId id, Email email, SecretHash codeHash, int attempts, Instant expiresAt, Instant consumedAt) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email is required");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash is required");
        this.attempts = attempts;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.consumedAt = consumedAt;
    }
}
