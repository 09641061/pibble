package com.takodu.api.assistant.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.infrastructure.persistence.jpa.entities.AssistantSettingsPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class AssistantSettingsPersistenceAssembler {
    private AssistantSettingsPersistenceAssembler() { }

    public static AssistantSettings toDomainFromPersistence(AssistantSettingsPersistenceEntity entity) {
        return new AssistantSettings(new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), entity.isEnabled(), entity.getBusinessInfo());
    }

    /** @param entity the stored row to update, or a new one */
    public static AssistantSettingsPersistenceEntity toPersistenceFromDomain(AssistantSettings settings,
                                                                            AssistantSettingsPersistenceEntity entity) {
        entity.setOrganizationId(settings.getOrganizationId().value());
        entity.setEstablishmentId(settings.getEstablishmentId().value());
        entity.setEnabled(settings.isEnabled());
        entity.setBusinessInfo(settings.getBusinessInfo());
        return entity;
    }
}
