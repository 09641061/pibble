package com.takodu.api.customers.infrastructure.persistence.jpa.repositories;

import com.takodu.api.customers.infrastructure.persistence.jpa.entities.CustomerPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CustomerPersistenceRepository implements PanacheRepositoryBase<CustomerPersistenceEntity, UUID> {
    public Optional<CustomerPersistenceEntity> findActiveById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2 and deletedAt is null", organizationId, id).firstResultOptional();
    }

    public Optional<CustomerPersistenceEntity> findActiveByPhone(UUID organizationId, String phoneE164) {
        return find("organizationId = ?1 and phoneE164 = ?2 and deletedAt is null", organizationId, phoneE164)
                .firstResultOptional();
    }

    public Optional<CustomerPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
}
