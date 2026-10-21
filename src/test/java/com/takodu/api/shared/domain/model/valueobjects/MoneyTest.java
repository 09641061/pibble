package com.takodu.api.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void penBuildsSolesInCents() {
        assertEquals(new Money(4500, "PEN"), Money.pen(4500));
    }

    @Test
    void zeroIsAllowed() {
        assertEquals(0, Money.pen(0).cents());
    }

    @Test
    void negativeAmountIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> Money.pen(-1));

        assertEquals("Amount must not be negative", error.getMessage());
    }

    @Test
    void currencyMustBeAnIsoCode() {
        assertThrows(IllegalArgumentException.class, () -> new Money(100, "pen"));
        assertThrows(IllegalArgumentException.class, () -> new Money(100, "SOLES"));
        assertThrows(IllegalArgumentException.class, () -> new Money(100, null));
    }
}
