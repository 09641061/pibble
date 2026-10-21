package com.takodu.api.shared.domain.exceptions;

/**
 * A concurrent request with the same {@code Idempotency-Key} inserted first. Maps to HTTP 409 with code
 * {@code REQUEST_CONFLICT}; the client retries with the same key and gets the first request's result.
 */
public class RequestInProgressException extends ConflictException {

    public RequestInProgressException() {
        super("Request", "A request with this Idempotency-Key is still in progress; retry it");
    }
}
