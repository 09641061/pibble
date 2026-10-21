package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.MessagePersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.MessagePersistenceRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class MessageRepositoryImpl implements MessageRepository {
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

    /** A duplicate wamid cannot reach here: writers lock the conversation and check the wamid first (W4). */
    @Override
    public Message save(Message message) {
        var entity = MessagePersistenceAssembler.toPersistenceFromDomain(message);
        if (entity.getId() == null) {
            messagePersistenceRepository.persist(entity);
        } else {
            entity = messagePersistenceRepository.getEntityManager().merge(entity);
        }
        messagePersistenceRepository.flush();
        return MessagePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
