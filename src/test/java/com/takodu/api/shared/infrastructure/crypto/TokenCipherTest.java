package com.takodu.api.shared.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenCipherTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final TokenCipher cipher = new TokenCipher(KEY);

    @Test
    void decryptsWhatItEncrypted() {
        var encrypted = cipher.encrypt("EAAJB-business-token");

        assertFalse(encrypted.contains("EAAJB"));
        assertEquals("EAAJB-business-token", cipher.decrypt(encrypted));
    }

    @Test
    void sameTokenEncryptsDifferentlyEachTime() {
        assertNotEquals(cipher.encrypt("token"), cipher.encrypt("token"));
    }

    @Test
    void anotherKeyCannotDecrypt() {
        var other = new TokenCipher(Base64.getEncoder().encodeToString("another-key-of-exactly-32-bytes!".getBytes()));

        assertThrows(IllegalStateException.class, () -> other.decrypt(cipher.encrypt("token")));
    }

    @Test
    void keyMustBe32Bytes() {
        assertThrows(IllegalStateException.class, () -> new TokenCipher(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
