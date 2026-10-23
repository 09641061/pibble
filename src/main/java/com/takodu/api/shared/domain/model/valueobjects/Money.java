package com.takodu.api.shared.domain.model.valueobjects;

/**
 * An amount in the smallest unit of its currency, so prices never lose precision.
 *
 * @param cents amount in cents, never negative
 * @param currency ISO 4217 code, for example {@code PEN}
 */
public record Money(long cents, String currency) {
    public Money {
        if (cents < 0) throw new IllegalArgumentException("Amount must not be negative");
        if (currency == null || !currency.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("Currency must be an ISO 4217 code");
    }

    /**
     * @param cents amount in céntimos
     * @return the amount in Peruvian soles
     */
    public static Money pen(long cents) {
        return new Money(cents, "PEN");
    }
}
