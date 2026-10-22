package com.takodu.api.assistant.infrastructure.persistence.jpa.adapters;

import com.takodu.api.assistant.domain.exceptions.TurnAlreadyRunningException;
import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.assistant.domain.repositories.AssistantTurnRepository;
import com.takodu.api.assistant.infrastructure.persistence.jpa.assemblers.AssistantTurnPersistenceAssembler;
import com.takodu.api.assistant.infrastructure.persistence.jpa.repositories.AssistantTurnPersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class AssistantTurnRepositoryImpl implements AssistantTurnRepository {
    private static final String RUNNING_UNIQUE = "assistant_turns_running_uk";

    private final AssistantTurnPersistenceRepository assistantTurnPersistenceRepository;

    public AssistantTurnRepositoryImpl(AssistantTurnPersistenceRepository assistantTurnPersistenceRepository) {
        this.assistantTurnPersistenceRepository = assistantTurnPersistenceRepository;
    }

    @Override
    public boolean existsByTriggerMessage(OrganizationId organizationId, MessageId triggerMessageId) {
        return assistantTurnPersistenceRepository.existsByTriggerMessage(organizationId.value(),
                triggerMessageId.value());
    }

    @Override
    public Optional<AssistantTurn> claimNext(Instant now) {
        return assistantTurnPersistenceRepository.claimNext(now, now.minus(AssistantTurn.LEASE))
                .map(AssistantTurnPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<AssistantTurn> lockById(OrganizationId organizationId, AssistantTurnId turnId) {
        return assistantTurnPersistenceRepository.lockById(organizationId.value(), turnId.value())
                .map(AssistantTurnPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean hasNewerPending(AssistantTurn turn) {
        return assistantTurnPersistenceRepository.hasNewerPending(turn.getOrganizationId().value(),
                turn.getConversationId().value(), turn.getId().value(), turn.getTriggerAt());
    }

    @Override
    public Optional<Instant> answeredThrough(OrganizationId organizationId, ConversationId conversationId) {
        return assistantTurnPersistenceRepository.answeredThrough(organizationId.value(), conversationId.value());
    }

    @Override
    public int countAnsweredSince(OrganizationId organizationId, ConversationId conversationId, Instant since) {
        return (int) assistantTurnPersistenceRepository.countAnsweredSince(organizationId.value(),
                conversationId.value(), since);
    }

    /**
     * {@code assistant_turns_trigger_uk} stays unmapped: the handler checks for the message's turn first, and the
     * webhook publishes a message once.
     */
    @Override
    public AssistantTurn save(AssistantTurn turn) {
        var entity = AssistantTurnPersistenceAssembler.toPersistenceFromDomain(turn);
        try {
            if (entity.getId() == null) {
                assistantTurnPersistenceRepository.persist(entity);
            } else {
                entity = assistantTurnPersistenceRepository.getEntityManager().merge(entity);
            }
            assistantTurnPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            if (RUNNING_UNIQUE.equals(e.getConstraintName())) throw new TurnAlreadyRunningException();
            throw e;
        }
        return AssistantTurnPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
