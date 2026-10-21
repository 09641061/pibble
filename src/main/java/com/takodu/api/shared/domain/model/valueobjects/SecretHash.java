package com.takodu.api.shared.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 of a secret the caller holds (a sign-in code, a refresh token or an invitation token). Only the hash is stored, so a
 * database leak does not reveal usable secrets.
 *
 * @param value lower-case hex digest
 */
public record SecretHash(String value) {
    public SecretHash {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Secret hash is invalid");
    }

    /**
     * @param secret the plain secret
     * @return its hash
     */
    public static SecretHash of(String secret) {
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("Secret is required");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every JDK", e);
        }
    }

    /**
     * @param other the hash to compare with
     * @return whether both hashes are equal, compared in constant time
     */
    public boolean matches(SecretHash other) {
        return MessageDigest.isEqual(value.getBytes(StandardCharsets.US_ASCII), other.value.getBytes(StandardCharsets.US_ASCII));
    }
}
