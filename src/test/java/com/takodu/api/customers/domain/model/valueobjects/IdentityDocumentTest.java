package com.takodu.api.customers.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityDocumentTest {

    @Test
    void documentIsOptional() {
        assertNull(IdentityDocument.optional(null, " "));
    }

    @Test
    void typeAndNumberGoTogether() {
        assertThrows(IllegalArgumentException.class, () -> IdentityDocument.optional("dni", null));
        assertThrows(IllegalArgumentException.class, () -> IdentityDocument.optional(null, "45678912"));
    }

    @Test
    void numberIsCheckedByFormat() {
        assertEquals(new IdentityDocument(IdentityDocument.Type.DNI, "45678912"), IdentityDocument.optional("DNI", "4567 8912"));
        assertEquals("AB12345", IdentityDocument.optional("passport", "ab12345").number());
        assertEquals("20123456789", IdentityDocument.optional("ruc", "20123456789").number());
        assertThrows(IllegalArgumentException.class, () -> IdentityDocument.optional("dni", "1234567"));
        assertThrows(IllegalArgumentException.class, () -> IdentityDocument.optional("ruc", "30123456789"));
    }

    @Test
    void unknownTypeIsRefused() {
        var error = assertThrows(IllegalArgumentException.class, () -> IdentityDocument.optional("licencia", "123456"));
        assertEquals("Document type must be dni, ruc, ce or passport", error.getMessage());
    }
}
