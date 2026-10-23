package com.takodu.api.iam.infrastructure.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SigningKeysCheckTest {

    @TempDir
    Path dir;

    @Test
    void readableKeyFilePasses() throws Exception {
        var key = Files.writeString(dir.resolve("private.pem"), "key");

        assertDoesNotThrow(() -> SigningKeysCheck.requireReadable("k", key.toString()));
        assertDoesNotThrow(() -> SigningKeysCheck.requireReadable("k", "file:" + key));
    }

    @Test
    void missingFileOrDirectoryStopsStartup() {
        assertThrows(IllegalStateException.class,
                () -> SigningKeysCheck.requireReadable("k", dir.resolve("missing.pem").toString()));
        assertThrows(IllegalStateException.class, () -> SigningKeysCheck.requireReadable("k", dir.toString()));
    }
}
