package com.takodu.api.shared.domain.exceptions;

/**
 * The credential the caller presented (a sign-in code, a refresh token) is wrong, used or expired. Maps to HTTP
 * 401; the client signs in again.
 */
public class AuthenticationFailedException extends DomainException {

    /**
     * @param code machine-readable error code, for example {@code LOGIN_CODE_INVALID}
     * @param message why the credential was refused, without revealing which part was wrong
     */
    public AuthenticationFailedException(String code, String message) {
        super(code, message);
    }
}
