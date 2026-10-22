package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.MessagePersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.MessagePersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MessageRepositoryImpl implements MessageRepository {
    private static final String IDEMPOTENCY_UNIQUE = "messages_idempotency_uk";

    private final MessagePersistenceRepository messagePersistenceRepository;

    public MessageRepositoryImpl(MessagePersistenceRepository messagePersistenceRepository) {
        this.messagePersistenceRepository = messagePersistenceRepository;
    }

    @Override
    public boolean existsByWamid(Wamid wamid) {
        return messagePersistenceRepository.existsByWamid(wamid.value());
    }

    @Override
    public Optional<Message> lockByWamid(Wamid wamid) {
        return messagePersistenceRepository.lockByWamid(wamid.value())
                .map(MessagePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Message> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return messagePersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(MessagePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Message> findByConversation(OrganizationId organizationId, ConversationId conversationId,
                                            PageCursor before, int limit) {
        return messagePersistenceRepository.findByConversation(organizationId.value(), conversationId.value(),
                        before == null ? null : before.at(), before == null ? null : before.id(), limit).stream()
                .map(MessagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Message> claimNextPending(Instant now) {
        return messagePersistenceRepository.claimNextPending(now).map(MessagePersistenceAssembler::toDomainFromPersistence);
    }

    /** A duplicate wamid cannot reach here: writers lock the conversation and check the wamid first (W4). */
    @Override
    public Message save(Message message) {
        var entity = MessagePersistenceAssembler.toPersistenceFromDomain(message);
        try {
            if (entity.getId() == null) {
                messagePersistenceRepository.persist(entity);
            } else {
                entity = messagePersistenceRepository.getEntityManager().merge(entity);
            }
            messagePersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return MessagePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
