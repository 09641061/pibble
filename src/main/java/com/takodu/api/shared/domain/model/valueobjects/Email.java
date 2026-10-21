package com.takodu.api.shared.domain.model.valueobjects;

import java.util.Locale;

/**
 * An email address, trimmed and lower-cased so {@code Ana@Gmail.com } and {@code ana@gmail.com} are one address
 * (I1). Users sign in with it and members are invited to it (O10).
 *
 * @param value the normalized address
 */
public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email is required");
        value = value.strip().toLowerCase(Locale.ROOT);
        int at = value.indexOf('@');
        String domain = at < 0 ? "" : value.substring(at + 1);
        boolean valid = at > 0 && at == value.lastIndexOf('@') && domain.contains(".")
                && !domain.startsWith(".") && !domain.endsWith(".") && value.length() <= 254
                && value.chars().noneMatch(Character::isWhitespace);
        if (!valid) throw new IllegalArgumentException("Email format is invalid");
    }
}
