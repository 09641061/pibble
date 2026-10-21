package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.ConversationPersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.ConversationPersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationPersistenceRepository conversationPersistenceRepository;

    public ConversationRepositoryImpl(ConversationPersistenceRepository conversationPersistenceRepository) {
        this.conversationPersistenceRepository = conversationPersistenceRepository;
    }

    @Override
    public Conversation lockOrStart(WhatsappLine line, CustomerId customerId, Instant firstMessageAt) {
        conversationPersistenceRepository.insertIfAbsent(line.getOrganizationId().value(),
                line.getEstablishmentId().value(), line.getId().value(), customerId.value(), firstMessageAt);
        return ConversationPersistenceAssembler.toDomainFromPersistence(
                conversationPersistenceRepository.lockByLineAndCustomer(line.getId().value(), customerId.value()));
    }

    @Override
    public Conversation save(Conversation conversation) {
        var entity = ConversationPersistenceAssembler.toPersistenceFromDomain(conversation);
        if (entity.getId() == null) {
            conversationPersistenceRepository.persist(entity);
        } else {
            entity = conversationPersistenceRepository.getEntityManager().merge(entity);
        }
        conversationPersistenceRepository.flush();
        return ConversationPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
