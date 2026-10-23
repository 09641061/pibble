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

    /** A9: rows of day, turns, input, cached and output tokens, tool calls; the day is local to {@code zone}. */
    public List<Object[]> dailyUsage(UUID organizationId, UUID establishmentId, String zone, Instant from, Instant to) {
        @SuppressWarnings("unchecked") List<Object[]> rows = getEntityManager().createNativeQuery("""
                        select cast(finished_at at time zone :zone as date), count(*),
                               coalesce(sum(input_tokens), 0), coalesce(sum(cached_tokens), 0),
                               coalesce(sum(output_tokens), 0), coalesce(sum(tool_calls), 0)
                        from assistant_turns
                        where organization_id = :organizationId and establishment_id = :establishmentId
                          and status = 'done' and finished_at >= :from and finished_at < :to
                        group by 1 order by 1""")
                .setParameter("zone", zone)
                .setParameter("organizationId", organizationId)
                .setParameter("establishmentId", establishmentId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows;
    }

    public List<UUID> establishmentsWithUsage(UUID organizationId, Instant from, Instant to) {
        return getEntityManager().createQuery("""
                        select distinct t.establishmentId from AssistantTurnPersistenceEntity t
                        where t.organizationId = :organizationId and t.status = :done
                          and t.finishedAt >= :from and t.finishedAt < :to""", UUID.class)
                .setParameter("organizationId", organizationId)
                .setParameter("done", TurnStatus.DONE)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}
