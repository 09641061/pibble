package com.takodu.api.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void addressIsTrimmedAndLowerCased() {
        assertEquals(new Email("ana@gmail.com"), new Email("  Ana@Gmail.COM "));
    }

    @Test
    void malformedAddressesAreRejected() {
        for (String bad : new String[] {"ana", "@gmail.com", "ana@gmail", "ana@@gmail.com", "ana@.com", "ana@gmail.", "a na@gmail.com"}) {
            assertThrows(IllegalArgumentException.class, () -> new Email(bad), bad);
        }
    }

    @Test
    void blankAddressIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new Email(" "));

        assertEquals("Email is required", error.getMessage());
    }
}
