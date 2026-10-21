package com.takodu.api.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretHashTest {

    @Test
    void sameSecretGivesSameHash() {
        assertTrue(SecretHash.of("123456").matches(SecretHash.of("123456")));
        assertEquals(64, SecretHash.of("123456").value().length());
    }

    @Test
    void differentSecretsDoNotMatch() {
        assertFalse(SecretHash.of("123456").matches(SecretHash.of("123457")));
    }

    @Test
    void hashDoesNotContainTheSecret() {
        assertNotEquals("123456", SecretHash.of("123456").value());
    }
}
