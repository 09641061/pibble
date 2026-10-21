package com.takodu.api.customers.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * C1: a phone number in E.164 form, such as {@code +51987654321}.
 *
 * @param e164 the number with its country code and a leading {@code +}
 */
public record PhoneNumber(String e164) {
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{6,14}$");
    private static final Pattern PERUVIAN_MOBILE = Pattern.compile("^9[0-9]{8}$");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s().-]");

    public PhoneNumber {
        if (e164 == null || !E164.matcher(e164).matches())
            throw new IllegalArgumentException("Phone must be in E.164 format, such as +51987654321");
    }

    /**
     * Reads a number as staff type it: spaces, dots, dashes and parentheses are ignored, {@code 00} means {@code +},
     * and a Peruvian mobile typed without its country code ({@code 987 654 321}) gets {@code +51}.
     */
    public static PhoneNumber typed(String text) {
        if (text == null) throw new IllegalArgumentException("Phone is required");
        var digits = SEPARATORS.matcher(text).replaceAll("");
        if (digits.startsWith("00")) digits = "+" + digits.substring(2);
        if (PERUVIAN_MOBILE.matcher(digits).matches()) digits = "+51" + digits;
        if (!digits.startsWith("+"))
            throw new IllegalArgumentException("Type the phone with its country code, such as +51 987 654 321");
        return new PhoneNumber(digits);
    }

    /** C2: Meta's {@code wa_id} is the number without the {@code +}, such as {@code 51987654321}. */
    public static PhoneNumber fromWhatsappId(String waId) {
        if (waId == null) throw new IllegalArgumentException("WhatsApp id is required");
        return new PhoneNumber("+" + waId);
    }
}
