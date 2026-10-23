package com.takodu.api.shared.domain.exceptions;

import java.util.Locale;

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
     * @param words a name such as {@code "Time block"}
     * @return the words in upper snake case, such as {@code TIME_BLOCK}
     */
    protected static String codeOf(String words) {
        return words.strip().replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    /**
     * @return machine-readable error code
     */
    public String code() {
        return code;
    }
}
