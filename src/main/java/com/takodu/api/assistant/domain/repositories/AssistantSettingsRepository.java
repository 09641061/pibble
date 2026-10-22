package com.takodu.api.assistant.domain.repositories;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Optional;

public interface AssistantSettingsRepository {
    /** @return empty when nobody configured the establishment's assistant yet */
    Optional<AssistantSettings> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId);

    /**
     * Inserts the first settings of an establishment or updates them.
     *
     * @throws com.takodu.api.assistant.domain.exceptions.AssistantSettingsChangedException when a concurrent request
     *         inserted the first settings meanwhile
     */
    AssistantSettings save(AssistantSettings settings);
}
