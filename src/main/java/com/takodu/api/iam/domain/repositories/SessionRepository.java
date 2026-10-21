package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;

import java.util.Optional;

public interface SessionRepository {

    /**
     * Locks the row until the transaction ends, so two refreshes with one token are applied one after the other.
     *
     * @return the session whose current or previous refresh token has this hash
     */
    Optional<Session> findByRefreshTokenForUpdate(SecretHash refreshTokenHash);

    Session save(Session session);
}
