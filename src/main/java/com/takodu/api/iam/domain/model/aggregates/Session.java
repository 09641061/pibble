package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * I5: one signed-in device, held by a refresh token valid 30 days. Only the token's hash is stored, and a user
 * may hold several sessions.
 */
@Getter
public class Session {
    public static final Duration TTL = Duration.ofDays(30);

    private final SessionId id;
    private final UserId userId;
    private SecretHash refreshTokenHash;
    private SecretHash previousRefreshTokenHash;
    private Instant rotatedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant lastUsedAt;

    /**
     * @param userId who signed in
     * @param refreshTokenHash hash of the refresh token returned to the device
     * @param now the current instant
     * @return a session expiring after {@link #TTL}
     */
    public static Session open(UserId userId, SecretHash refreshTokenHash, Instant now) {
        return new Session(null, userId, refreshTokenHash, null, null, now.plus(TTL), null, now);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Session(SessionId id, UserId userId, SecretHash refreshTokenHash, SecretHash previousRefreshTokenHash,
                   Instant rotatedAt, Instant expiresAt, Instant revokedAt, Instant lastUsedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash is required");
        this.previousRefreshTokenHash = previousRefreshTokenHash;
        this.rotatedAt = rotatedAt;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.revokedAt = revokedAt;
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt is required");
    }

    /**
     * @param now the current instant
     * @return whether the session is neither revoked nor expired
     */
    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
