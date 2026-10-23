package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.MessagePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MessagePersistenceRepository implements PanacheRepositoryBase<MessagePersistenceEntity, UUID> {
    public boolean existsByWamid(String wamid) {
        return count("wamid", wamid) > 0;
    }

    public Optional<MessagePersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public Optional<MessagePersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }

    /** Keyset paging on {@code messages_thread_idx}: newest first, then id. */
    public List<MessagePersistenceEntity> findByConversation(UUID organizationId, UUID conversationId, Instant beforeAt,
                                                             UUID beforeId, int limit) {
        var page = beforeAt == null
                ? find("organizationId = ?1 and conversationId = ?2 order by occurredAt desc, id desc",
                        organizationId, conversationId)
                : find("organizationId = ?1 and conversationId = ?2 and (occurredAt < ?3 or "
                                + "(occurredAt = ?3 and id < ?4)) order by occurredAt desc, id desc",
                        organizationId, conversationId, beforeAt, beforeId);
        return page.page(0, limit).list();
    }

    /** {@code SKIP LOCKED} lets several senders work at once without waiting for each other's row. */
    public Optional<MessagePersistenceEntity> claimNextPending(Instant now) {
        @SuppressWarnings("unchecked")
        List<MessagePersistenceEntity> claimed = getEntityManager().createNativeQuery("""
                        SELECT * FROM messages
                        WHERE status = 'pending' AND next_attempt_at <= :now
                        ORDER BY next_attempt_at, id
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED""", MessagePersistenceEntity.class)
                .setParameter("now", now)
                .getResultList();
        return claimed.stream().findFirst();
    }

    /** Uses {@code messages_media_due_idx}; {@code SKIP LOCKED} as for the outbox. */
    public Optional<MessagePersistenceEntity> claimNextMediaDue(Instant now) {
        @SuppressWarnings("unchecked")
        List<MessagePersistenceEntity> claimed = getEntityManager().createNativeQuery("""
                        SELECT * FROM messages
                        WHERE media_status = 'pending' AND media_next_attempt_at <= :now
                        ORDER BY media_next_attempt_at, id
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED""", MessagePersistenceEntity.class)
                .setParameter("now", now)
                .getResultList();
        return claimed.stream().findFirst();
    }

    public Optional<MessagePersistenceEntity> lockByWamid(String wamid) {
        return find("wamid", wamid).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}
