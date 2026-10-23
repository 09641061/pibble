package com.takodu.api.shared.domain.exceptions;

/**
 * The caller exceeded a rate limit set by a business rule, such as five sign-in codes per hour (I3). Maps to
 * HTTP 429; the client waits and tries again.
 */
public class TooManyRequestsException extends DomainException {

    /**
     * @param code machine-readable error code, for example {@code LOGIN_CODE_LIMIT}
     * @param message which limit was reached
     */
    public TooManyRequestsException(String code, String message) {
        super(code, message);
    }
}
