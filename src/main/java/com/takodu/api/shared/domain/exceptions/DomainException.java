package com.takodu.api.shared.domain.exceptions;

/**
 * Base type for expected business failures.
 *
 * <p>Each subclass maps to one HTTP status in
 * {@code shared.interfaces.rest.errors.GlobalExceptionMapper}. Bounded contexts extend a subclass
 * (for example {@code UserNotFoundException extends ResourceNotFoundException}) instead of this class.</p>
 */
public abstract class DomainException extends RuntimeException {
    private final String code;

    /**
     * @param code machine-readable error code, upper snake case (for example {@code USER_NOT_FOUND})
     * @param message human-readable message returned to the client
     */
    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @return machine-readable error code
     */
    public String code() {
        return code;
    }
}
