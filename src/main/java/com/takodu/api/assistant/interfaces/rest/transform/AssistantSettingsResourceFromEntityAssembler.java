package com.takodu.api.assistant.interfaces.rest.transform;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantSettingsResource;

public final class AssistantSettingsResourceFromEntityAssembler {
    private AssistantSettingsResourceFromEntityAssembler() { }

    public static AssistantSettingsResource toResourceFromEntity(AssistantSettings settings) {
        return new AssistantSettingsResource(settings.getEstablishmentId().value(), settings.isEnabled(),
                settings.getBusinessInfo());
    }
}
