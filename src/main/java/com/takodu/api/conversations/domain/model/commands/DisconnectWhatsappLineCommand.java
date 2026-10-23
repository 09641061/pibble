package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public record DisconnectWhatsappLineCommand(OrganizationId organizationId, EstablishmentId establishmentId) {
    public DisconnectWhatsappLineCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
