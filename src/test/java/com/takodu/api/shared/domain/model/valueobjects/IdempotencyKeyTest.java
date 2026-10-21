package com.takodu.api.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdempotencyKeyTest {

    @Test
    void missingHeaderIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(null));

        assertEquals("Idempotency-Key header is required", error.getMessage());
    }
}
