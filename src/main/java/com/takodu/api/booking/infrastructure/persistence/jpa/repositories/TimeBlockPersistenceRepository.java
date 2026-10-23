package com.takodu.api.booking.infrastructure.persistence.jpa.repositories;

import com.takodu.api.booking.infrastructure.persistence.jpa.entities.TimeBlockPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TimeBlockPersistenceRepository implements PanacheRepositoryBase<TimeBlockPersistenceEntity, UUID> {
    public Optional<TimeBlockPersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public Optional<TimeBlockPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }

    public List<TimeBlockPersistenceEntity> findOverlapping(UUID organizationId, UUID establishmentId, Instant start,
                                                            Instant end) {
        return list("organizationId = ?1 and establishmentId = ?2 and startsAt < ?4 and endsAt > ?3 order by startsAt, id",
                organizationId, establishmentId, start, end);
    }
}
