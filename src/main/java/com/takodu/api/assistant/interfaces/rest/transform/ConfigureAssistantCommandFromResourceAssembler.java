package com.takodu.api.assistant.interfaces.rest.transform;

import com.takodu.api.assistant.domain.model.commands.ConfigureAssistantCommand;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantSettingsDetailsResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class ConfigureAssistantCommandFromResourceAssembler {
    private ConfigureAssistantCommandFromResourceAssembler() { }

    public static ConfigureAssistantCommand toCommandFromResource(OrganizationId organizationId, UUID establishmentId,
                                                                  AssistantSettingsDetailsResource resource) {
        return new ConfigureAssistantCommand(organizationId, new EstablishmentId(establishmentId), resource.enabled(),
                resource.businessInfo());
    }
}
