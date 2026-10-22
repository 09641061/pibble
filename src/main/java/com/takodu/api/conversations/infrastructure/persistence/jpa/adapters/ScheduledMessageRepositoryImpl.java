package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.model.aggregates.ScheduledMessage;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.repositories.ScheduledMessageRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.ScheduledMessagePersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.ScheduledMessagePersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ScheduledMessageRepositoryImpl implements ScheduledMessageRepository {
    private final ScheduledMessagePersistenceRepository scheduledMessagePersistenceRepository;

    public ScheduledMessageRepositoryImpl(ScheduledMessagePersistenceRepository scheduledMessagePersistenceRepository) {
        this.scheduledMessagePersistenceRepository = scheduledMessagePersistenceRepository;
    }

    @Override
    public List<ScheduledMessage> lockByAppointment(OrganizationId organizationId, AppointmentId appointmentId) {
        return scheduledMessagePersistenceRepository.lockByAppointment(organizationId.value(), appointmentId.value())
                .stream().map(ScheduledMessagePersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<ScheduledMessage> claimNextDue(Instant now) {
        return scheduledMessagePersistenceRepository.claimNextDue(now)
                .map(ScheduledMessagePersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * A second row for the same appointment and kind leaves {@code scheduled_messages_appointment_kind_uk} unmapped:
     * the service looks for the row first, and of two concurrent changes to one appointment booking's version check
     * rolls one back, rows included.
     */
    @Override
    public ScheduledMessage save(ScheduledMessage message) {
        var entity = ScheduledMessagePersistenceAssembler.toPersistenceFromDomain(message);
        if (entity.getId() == null) {
            scheduledMessagePersistenceRepository.persist(entity);
        } else {
            entity = scheduledMessagePersistenceRepository.getEntityManager().merge(entity);
        }
        scheduledMessagePersistenceRepository.flush();
        return ScheduledMessagePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
