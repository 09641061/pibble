package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.ConversationPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    public Optional<ConversationPersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public Optional<ConversationPersistenceEntity> lockById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }

    /** Keyset paging on {@code conversations_inbox_idx}: newest activity first, then id. */
    public List<ConversationPersistenceEntity> findByEstablishment(UUID organizationId, UUID establishmentId,
                                                                   Instant afterAt, UUID afterId, int limit) {
        var page = afterAt == null
                ? find("organizationId = ?1 and establishmentId = ?2 order by lastMessageAt desc, id desc",
                        organizationId, establishmentId)
                : find("organizationId = ?1 and establishmentId = ?2 and (lastMessageAt < ?3 or "
                                + "(lastMessageAt = ?3 and id < ?4)) order by lastMessageAt desc, id desc",
                        organizationId, establishmentId, afterAt, afterId);
        return page.page(0, limit).list();
    }

    public ConversationPersistenceEntity lockByLineAndCustomer(UUID lineId, UUID customerId) {
        return find("lineId = ?1 and customerId = ?2", lineId, customerId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .singleResult();
    }
}
