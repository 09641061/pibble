package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    /**
     * Locks the row until the transaction ends, so two refreshes with one token are applied one after the other.
     *
     * @return the session whose current or previous refresh token has this hash
     */
    Optional<Session> findByRefreshTokenForUpdate(SecretHash refreshTokenHash);

    /** @return the user's sessions that are neither revoked nor expired at {@code now}, most recently used first */
    List<Session> findActiveByUser(UserId userId, Instant now);

    /** @return the session if it belongs to the user; another user's session is not found */
    Optional<Session> findByIdAndUser(SessionId sessionId, UserId userId);

    Session save(Session session);

    /** @return how many sessions that expired or were revoked before {@code instant} were deleted */
    long deleteEndedBefore(Instant instant);
}
