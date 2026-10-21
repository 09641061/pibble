package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.WhatsappLinePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WhatsappLinePersistenceRepository implements PanacheRepositoryBase<WhatsappLinePersistenceEntity, UUID> {
    public Optional<WhatsappLinePersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public Optional<WhatsappLinePersistenceEntity> findLatestByEstablishment(UUID organizationId, UUID establishmentId) {
        return find("organizationId = ?1 and establishmentId = ?2 order by case when status = ?3 then 0 else 1 end, "
                        + "connectedAt desc", organizationId, establishmentId, WhatsappLinePersistenceEntity.CONNECTED)
                .firstResultOptional();
    }

    public Optional<WhatsappLinePersistenceEntity> findByLastIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and lastIdempotencyKey = ?2", organizationId, key).firstResultOptional();
    }

    public Optional<WhatsappLinePersistenceEntity> findByPhoneNumberId(String phoneNumberId) {
        return find("phoneNumberId", phoneNumberId).firstResultOptional();
    }

    public List<WhatsappLinePersistenceEntity> findConnectedByWabaId(String wabaId) {
        return list("wabaId = ?1 and status = ?2", wabaId, WhatsappLinePersistenceEntity.CONNECTED);
    }
}
