package com.takodu.api.assistant.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public record GetAssistantSettingsQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetAssistantSettingsQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
