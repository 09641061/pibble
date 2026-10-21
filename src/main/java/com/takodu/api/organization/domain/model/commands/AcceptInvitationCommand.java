package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * O10: the signed-in user joins with the token from the invitation email.
 *
 * @param userId the caller, from the access token
 * @param token the plain token from the link
 */
public record AcceptInvitationCommand(UserId userId, String token) {
    public AcceptInvitationCommand {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token is required");
    }
}
