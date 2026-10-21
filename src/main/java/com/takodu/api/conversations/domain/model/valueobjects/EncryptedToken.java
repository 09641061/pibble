package com.takodu.api.conversations.domain.model.valueobjects;

/**
 * The business token of a line, encrypted by the Meta adapter as soon as Meta returns it. Only the Meta adapter
 * decrypts it, so the plain token never reaches the domain, a log or an endpoint.
 *
 * @param value the ciphertext
 */
public record EncryptedToken(String value) {
    public EncryptedToken {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("The token is required");
    }

    @Override
    public String toString() {
        return "EncryptedToken[***]";
    }
}
