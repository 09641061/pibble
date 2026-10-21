package com.takodu.api.customers.domain.model.commands;

import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * C2: a WhatsApp message arrived from this number; creates the customer the first time.
 *
 * @param organizationId derived from the WhatsApp line, since webhooks have no {@code TenantContext}
 * @param profileName the sender's WhatsApp profile name, optional
 */
public record RecordWhatsappContactCommand(OrganizationId organizationId, PhoneNumber phone, String profileName) {
    public RecordWhatsappContactCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (phone == null) throw new IllegalArgumentException("Phone is required");
    }
}
