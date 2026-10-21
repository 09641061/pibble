package com.takodu.api.iam.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "SessionTokens", description = "Send accessToken as a Bearer token; exchange refreshToken for new tokens before it expires")
public record SessionTokensResource(UUID sessionId, String accessToken, Instant accessTokenExpiresAt,
                                    String refreshToken, Instant refreshTokenExpiresAt, UserResource user) { }
