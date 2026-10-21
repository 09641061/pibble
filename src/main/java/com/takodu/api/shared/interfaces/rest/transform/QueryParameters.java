package com.takodu.api.shared.interfaces.rest.transform;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Parses date and time query parameters. JAX-RS answers 404 when it cannot convert a query parameter, which reads as
 * a missing resource; controllers take the text and parse it here, so a malformed value is a 400.
 */
public final class QueryParameters {
    private QueryParameters() { }

    /**
     * @throws IllegalArgumentException when the value is missing or not an ISO-8601 instant
     */
    public static Instant instant(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        try {
            return Instant.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " must be an instant such as 2026-11-02T15:00:00Z");
        }
    }

    /**
     * @throws IllegalArgumentException when the value is missing or not a yyyy-MM-dd date
     */
    public static LocalDate date(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " must be a date such as 2026-11-02");
        }
    }
}
