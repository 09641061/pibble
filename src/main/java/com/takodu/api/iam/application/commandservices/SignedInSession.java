package com.takodu.api.iam.application.commandservices;

import com.takodu.api.iam.application.internal.outboundservices.tokens.AccessToken;
import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.aggregates.User;

/**
 * The result of signing in or refreshing: the only moment the plain refresh token exists outside the device.
 *
 * @param user who signed in
 * @param session the device's session
 * @param refreshToken the plain refresh token; only its hash is stored
 * @param accessToken the signed access token
 */
public record SignedInSession(User user, Session session, String refreshToken, AccessToken accessToken) { }
