package com.takodu.api.shared.domain.exceptions;

/**
 * The caller's role or establishments do not allow the operation. Maps to HTTP 403.
 */
public class AccessDeniedException extends DomainException {

    /**
     * @param action what the caller tried to do, for example {@code "delete customers"}
     */
    public AccessDeniedException(String action) {
        super("ACCESS_DENIED", "Not allowed to " + action);
    }
}
