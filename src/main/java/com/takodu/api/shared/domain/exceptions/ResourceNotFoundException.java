package com.takodu.api.shared.domain.exceptions;

/**
 * The requested aggregate does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends DomainException {

    /**
     * @param resource aggregate name, for example {@code "Time block"}; produces code {@code TIME_BLOCK_NOT_FOUND}
     * @param identifier the identifier that was looked up
     */
    public ResourceNotFoundException(String resource, Object identifier) {
        super(codeOf(resource) + "_NOT_FOUND", "%s not found: %s".formatted(resource, identifier));
    }
}
