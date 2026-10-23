package com.takodu.api.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifiersTest {

    @Test
    void missingValueIsRejectedWithTheTypeName() {
        var error = assertThrows(IllegalArgumentException.class, () -> new OrganizationId(null));

        assertEquals("OrganizationId is required", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new EstablishmentId(null));
        assertThrows(IllegalArgumentException.class, () -> new MemberId(null));
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(null));
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertThrows(IllegalArgumentException.class, () -> new ServiceId(null));
    }

    @Test
    void identifiersAreEqualByValue() {
        var uuid = UUID.randomUUID();

        assertEquals(new CustomerId(uuid), new CustomerId(uuid));
        assertNotEquals(new CustomerId(uuid), new CustomerId(UUID.randomUUID()));
    }
}
