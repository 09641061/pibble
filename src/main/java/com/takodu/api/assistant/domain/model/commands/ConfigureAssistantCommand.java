package com.takodu.api.assistant.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param businessInfo null clears it */
public record ConfigureAssistantCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                        boolean enabled, String businessInfo) {
    public ConfigureAssistantCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
