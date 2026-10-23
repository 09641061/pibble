package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.ConversationPersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.ConversationPersistenceRepository;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    public Optional<Conversation> findById(OrganizationId organizationId, ConversationId conversationId) {
        return conversationPersistenceRepository.findById(organizationId.value(), conversationId.value())
                .map(ConversationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Conversation> lockById(OrganizationId organizationId, ConversationId conversationId) {
        return conversationPersistenceRepository.lockById(organizationId.value(), conversationId.value())
                .map(ConversationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Conversation> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId,
                                                  PageCursor after, int limit) {
        return conversationPersistenceRepository.findByEstablishment(organizationId.value(), establishmentId.value(),
                        after == null ? null : after.at(), after == null ? null : after.id(), limit).stream()
                .map(ConversationPersistenceAssembler::toDomainFromPersistence)
                .toList();
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
