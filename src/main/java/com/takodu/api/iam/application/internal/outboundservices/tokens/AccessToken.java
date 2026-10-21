package com.takodu.api.iam.application.internal.outboundservices.tokens;

import java.time.Instant;

/**
 * A signed, short-lived token the client sends as {@code Authorization: Bearer}.
 *
 * @param value the compact JWT
 * @param expiresAt when it stops being accepted
 */
public record AccessToken(String value, Instant expiresAt) { }
