package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.exceptions.InvalidRefreshTokenException;
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
    /** How long the previous refresh token is accepted again, so a refresh whose response was lost can be retried. */
    public static final Duration RETRY_WINDOW = Duration.ofMinutes(2);

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

    /**
     * I5: exchanges the presented refresh token for the next one and extends the session by {@link #TTL}.
     *
     * <p>The token replaced by the last rotation is accepted again within {@link #RETRY_WINDOW} of that
     * rotation, which covers a device whose refresh response was lost; the window does not restart on such a
     * retry. Presenting it later means two parties hold the session, so it is revoked.</p>
     *
     * @param presented hash of the token the device sent
     * @param next hash of the token that replaces it
     * @param now the current instant
     * @throws InvalidRefreshTokenException when the session is revoked or expired, the token is unknown, or an
     *         old token is reused after the window, which also revokes the session
     */
    public void refresh(SecretHash presented, SecretHash next, Instant now) {
        if (!isActive(now)) throw new InvalidRefreshTokenException();
        boolean current = refreshTokenHash.matches(presented);
        boolean previous = !current && previousRefreshTokenHash != null && previousRefreshTokenHash.matches(presented);
        if (!current && !previous) throw new InvalidRefreshTokenException();
        if (previous && now.isAfter(rotatedAt.plus(RETRY_WINDOW))) {
            revoke(now);
            throw new InvalidRefreshTokenException();
        }
        if (current) {
            previousRefreshTokenHash = refreshTokenHash;
            rotatedAt = now;
        }
        refreshTokenHash = next;
        expiresAt = now.plus(TTL);
        lastUsedAt = now;
    }

    /**
     * Ends the session; its refresh token stops working at once and its access tokens within 15 minutes.
     *
     * @param now the current instant
     */
    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = now;
    }
}
