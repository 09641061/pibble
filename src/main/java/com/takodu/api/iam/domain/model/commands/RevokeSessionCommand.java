package com.takodu.api.iam.domain.model.commands;

import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * Sign one device out.
 *
 * @param userId the caller, from the access token
 * @param sessionId the session to end
 */
public record RevokeSessionCommand(UserId userId, SessionId sessionId) {
    public RevokeSessionCommand {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (sessionId == null) throw new IllegalArgumentException("sessionId is required");
    }
}
