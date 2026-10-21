package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.ConversationPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ConversationPersistenceRepository implements PanacheRepositoryBase<ConversationPersistenceEntity, UUID> {

    /** Waits for a concurrent insert of the same pair, then does nothing; {@code conversations_line_customer_uk} decides. */
    public void insertIfAbsent(UUID organizationId, UUID establishmentId, UUID lineId, UUID customerId,
                               Instant lastMessageAt) {
        getEntityManager().createNativeQuery("""
                        INSERT INTO conversations (organization_id, establishment_id, line_id, customer_id, last_message_at)
                        VALUES (:organizationId, :establishmentId, :lineId, :customerId, :lastMessageAt)
                        ON CONFLICT (line_id, customer_id) DO NOTHING""")
                .setParameter("organizationId", organizationId)
                .setParameter("establishmentId", establishmentId)
                .setParameter("lineId", lineId)
                .setParameter("customerId", customerId)
                .setParameter("lastMessageAt", lastMessageAt)
                .executeUpdate();
    }

    public ConversationPersistenceEntity lockByLineAndCustomer(UUID lineId, UUID customerId) {
        return find("lineId = ?1 and customerId = ?2", lineId, customerId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .singleResult();
    }
}
