package com.takodu.api.assistant.infrastructure.persistence.jpa.repositories;

import com.takodu.api.assistant.infrastructure.persistence.jpa.entities.AssistantSettingsPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AssistantSettingsPersistenceRepository
        implements PanacheRepositoryBase<AssistantSettingsPersistenceEntity, UUID> {

    public Optional<AssistantSettingsPersistenceEntity> findByEstablishment(UUID organizationId, UUID establishmentId) {
        return find("organizationId = ?1 and establishmentId = ?2", organizationId, establishmentId)
                .firstResultOptional();
    }
}
