package com.takodu.api.assistant.infrastructure.persistence.jpa.adapters;

import com.takodu.api.assistant.domain.exceptions.AssistantSettingsChangedException;
import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.repositories.AssistantSettingsRepository;
import com.takodu.api.assistant.infrastructure.persistence.jpa.assemblers.AssistantSettingsPersistenceAssembler;
import com.takodu.api.assistant.infrastructure.persistence.jpa.entities.AssistantSettingsPersistenceEntity;
import com.takodu.api.assistant.infrastructure.persistence.jpa.repositories.AssistantSettingsPersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;

/** The settings are keyed by establishment, so the adapter updates the stored row instead of tracking its id. */
@ApplicationScoped
public class AssistantSettingsRepositoryImpl implements AssistantSettingsRepository {
    private static final String ESTABLISHMENT_UNIQUE = "assistant_settings_establishment_uk";

    private final AssistantSettingsPersistenceRepository assistantSettingsPersistenceRepository;

    public AssistantSettingsRepositoryImpl(AssistantSettingsPersistenceRepository assistantSettingsPersistenceRepository) {
        this.assistantSettingsPersistenceRepository = assistantSettingsPersistenceRepository;
    }

    @Override
    public Optional<AssistantSettings> findByEstablishment(OrganizationId organizationId,
                                                           EstablishmentId establishmentId) {
        return assistantSettingsPersistenceRepository.findByEstablishment(organizationId.value(), establishmentId.value())
                .map(AssistantSettingsPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public AssistantSettings save(AssistantSettings settings) {
        var entity = assistantSettingsPersistenceRepository.findByEstablishment(settings.getOrganizationId().value(),
                settings.getEstablishmentId().value()).orElseGet(AssistantSettingsPersistenceEntity::new);
        AssistantSettingsPersistenceAssembler.toPersistenceFromDomain(settings, entity);
        try {
            if (entity.getId() == null) assistantSettingsPersistenceRepository.persist(entity);
            assistantSettingsPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            if (ESTABLISHMENT_UNIQUE.equals(e.getConstraintName())) throw new AssistantSettingsChangedException();
            throw e;
        }
        return AssistantSettingsPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
