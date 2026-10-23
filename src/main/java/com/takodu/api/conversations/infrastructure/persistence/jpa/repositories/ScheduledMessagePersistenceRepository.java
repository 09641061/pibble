package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.ScheduledMessagePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ScheduledMessagePersistenceRepository
        implements PanacheRepositoryBase<ScheduledMessagePersistenceEntity, UUID> {

    public List<ScheduledMessagePersistenceEntity> lockByAppointment(UUID organizationId, UUID appointmentId) {
        return find("organizationId = ?1 and appointmentId = ?2", organizationId, appointmentId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).list();
    }

    /** {@code SKIP LOCKED} lets several dispatchers work at once without waiting for each other's row. */
    public Optional<ScheduledMessagePersistenceEntity> claimNextDue(Instant now) {
        @SuppressWarnings("unchecked")
        List<ScheduledMessagePersistenceEntity> claimed = getEntityManager().createNativeQuery("""
                        SELECT * FROM scheduled_messages
                        WHERE status = 'pending' AND send_at <= :now
                        ORDER BY send_at, id
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED""", ScheduledMessagePersistenceEntity.class)
                .setParameter("now", now)
                .getResultList();
        return claimed.stream().findFirst();
    }
}
