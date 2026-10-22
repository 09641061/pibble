package com.takodu.api.assistant.infrastructure.persistence.jpa.repositories;

import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import com.takodu.api.assistant.infrastructure.persistence.jpa.entities.AssistantTurnPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AssistantTurnPersistenceRepository implements PanacheRepositoryBase<AssistantTurnPersistenceEntity, UUID> {

    public boolean existsByTriggerMessage(UUID organizationId, UUID triggerMessageId) {
        return count("organizationId = ?1 and triggerMessageId = ?2", organizationId, triggerMessageId) > 0;
    }

    /**
     * {@code SKIP LOCKED} lets several workers claim at once; a chat with another running turn is left out, and
     * {@code assistant_turns_running_uk} catches two workers starting turns of one chat at the same moment.
     */
    public Optional<AssistantTurnPersistenceEntity> claimNext(Instant now, Instant staleBefore) {
        @SuppressWarnings("unchecked")
        List<AssistantTurnPersistenceEntity> claimed = getEntityManager().createNativeQuery("""
                        SELECT * FROM assistant_turns t
                        WHERE ((t.status = 'pending' AND t.run_after <= :now)
                               OR (t.status = 'running' AND t.started_at <= :staleBefore))
                          AND NOT EXISTS (SELECT 1 FROM assistant_turns r
                                          WHERE r.conversation_id = t.conversation_id
                                            AND r.status = 'running' AND r.id <> t.id)
                        ORDER BY t.run_after, t.trigger_at, t.id
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED""", AssistantTurnPersistenceEntity.class)
                .setParameter("now", now)
                .setParameter("staleBefore", staleBefore)
                .getResultList();
        return claimed.stream().findFirst();
    }

    public Optional<AssistantTurnPersistenceEntity> lockById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    public boolean hasNewerPending(UUID organizationId, UUID conversationId, UUID id, Instant triggerAt) {
        return count("organizationId = ?1 and conversationId = ?2 and id <> ?3 and status = ?4 and triggerAt > ?5",
                organizationId, conversationId, id, TurnStatus.PENDING, triggerAt) > 0;
    }

    public Optional<Instant> answeredThrough(UUID organizationId, UUID conversationId) {
        return getEntityManager().createQuery("""
                        select max(t.answeredThrough) from AssistantTurnPersistenceEntity t
                        where t.organizationId = :organizationId and t.conversationId = :conversationId
                          and t.status = :done""", Instant.class)
                .setParameter("organizationId", organizationId)
                .setParameter("conversationId", conversationId)
                .setParameter("done", TurnStatus.DONE)
                .getResultStream().filter(Objects::nonNull).findFirst();
    }

    public long countAnsweredSince(UUID organizationId, UUID conversationId, Instant since) {
        return count("organizationId = ?1 and conversationId = ?2 and status = ?3 and finishedAt >= ?4",
                organizationId, conversationId, TurnStatus.DONE, since);
    }
}
