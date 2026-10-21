package com.takodu.api.shared.domain.exceptions;

/**
 * The operation collides with existing state, such as a duplicate unique value. Maps to HTTP 409.
 */
public class ConflictException extends DomainException {

    /**
     * @param resource aggregate name, for example {@code "User"}; produces code {@code USER_CONFLICT}
     * @param message what conflicts
     */
    public ConflictException(String resource, String message) {
        super(codeOf(resource) + "_CONFLICT", message);
    }
}
