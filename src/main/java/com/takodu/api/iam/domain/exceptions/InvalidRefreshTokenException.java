package com.takodu.api.iam.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;

/**
 * I5: the refresh token is unknown, expired, revoked or was reused after its retry window; the device signs in
 * again.
 */
public class InvalidRefreshTokenException extends AuthenticationFailedException {

    public InvalidRefreshTokenException() {
        super("REFRESH_TOKEN_INVALID", "The session has ended; sign in again");
    }
}
