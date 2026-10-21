package com.takodu.api.iam.domain.model.commands;

/**
 * Exchange a refresh token for new tokens.
 *
 * @param refreshToken the plain token the device holds
 */
public record RefreshSessionCommand(String refreshToken) {
    public RefreshSessionCommand {
        if (refreshToken == null || refreshToken.isBlank()) throw new IllegalArgumentException("refreshToken is required");
    }
}
