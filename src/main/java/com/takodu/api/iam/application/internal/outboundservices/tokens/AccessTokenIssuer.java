package com.takodu.api.iam.application.internal.outboundservices.tokens;

import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.time.Duration;
import java.time.Instant;

/** Signs access tokens; verification is done by the framework on every request. */
public interface AccessTokenIssuer {
    /** I5: access tokens live 15 minutes, so a revoked session stops working within that time. */
    Duration TTL = Duration.ofMinutes(15);

    /**
     * @param userId the token's subject
     * @param sessionId the session that issued it, carried as the {@code sid} claim
     * @param now the current instant
     * @return a token expiring after {@link #TTL}
     */
    AccessToken issue(UserId userId, SessionId sessionId, Instant now);
}
