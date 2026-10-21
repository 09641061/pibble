package com.takodu.api.customers.domain.model.valueobjects;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * C4: an optional identity document, checked by format only; nothing looks it up online.
 *
 * @param number upper-case, without spaces
 */
public record IdentityDocument(Type type, String number) {
    public enum Type { DNI, RUC, CE, PASSPORT }

    public IdentityDocument {
        Objects.requireNonNull(type, "Document type is required");
        number = number == null ? "" : number.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        boolean valid = switch (type) {
            case DNI -> number.matches("\\d{8}");
            case RUC -> number.matches("(10|15|17|20)\\d{9}");
            case CE -> number.matches("[A-Z0-9]{9,12}");
            case PASSPORT -> number.matches("[A-Z0-9]{6,15}");
        };
        if (!valid) throw new IllegalArgumentException("Invalid " + type.name().toLowerCase(Locale.ROOT) + " number");
    }

    /**
     * C4: type and number come together or not at all.
     *
     * @param type {@code dni}, {@code ruc}, {@code ce} or {@code passport}, in any case
     * @return the document, or {@code null} when both are blank
     */
    public static IdentityDocument optional(String type, String number) {
        boolean noType = type == null || type.isBlank();
        boolean noNumber = number == null || number.isBlank();
        if (noType && noNumber) return null;
        if (noType || noNumber) throw new IllegalArgumentException("Document type and number go together");
        var parsed = Arrays.stream(Type.values()).filter(t -> t.name().equalsIgnoreCase(type.strip())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Document type must be dni, ruc, ce or passport"));
        return new IdentityDocument(parsed, number);
    }
}
