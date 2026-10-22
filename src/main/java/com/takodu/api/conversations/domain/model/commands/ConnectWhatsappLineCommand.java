package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.regex.Pattern;

/**
 * W2: what Embedded Signup returned to the owner's browser.
 *
 * @param code the single-use code Meta exchanges for the business token
 * @param pin the two-step verification PIN that registers a {@code CLOUD_API} number; null in coexistence
 */
public record ConnectWhatsappLineCommand(OrganizationId organizationId, EstablishmentId establishmentId, String code,
                                         String wabaId, String phoneNumberId, LineMode mode, String pin,
                                         IdempotencyKey idempotencyKey) {
    private static final Pattern PIN = Pattern.compile("\\d{6}");
    private static final Pattern META_ID = Pattern.compile("\\d{1,32}");

    public ConnectWhatsappLineCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (wabaId == null || !META_ID.matcher(wabaId).matches())
            throw new IllegalArgumentException("wabaId must be Meta's numeric id");
        if (phoneNumberId == null || !META_ID.matcher(phoneNumberId).matches())
            throw new IllegalArgumentException("phoneNumberId must be Meta's numeric id");
        if (mode == null) mode = LineMode.COEXISTENCE;
        if (mode == LineMode.CLOUD_API && (pin == null || !PIN.matcher(pin).matches()))
            throw new IllegalArgumentException("A cloud_api number needs a 6-digit pin");
        if (mode == LineMode.COEXISTENCE) pin = null;
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
