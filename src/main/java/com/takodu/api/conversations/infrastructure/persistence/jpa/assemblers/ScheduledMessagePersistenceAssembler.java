package com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.conversations.domain.model.aggregates.ScheduledMessage;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.ScheduledMessageId;
import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.ScheduledMessagePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

public final class ScheduledMessagePersistenceAssembler {
    private ScheduledMessagePersistenceAssembler() { }

    public static ScheduledMessage toDomainFromPersistence(ScheduledMessagePersistenceEntity entity) {
        return new ScheduledMessage(new ScheduledMessageId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), new AppointmentId(entity.getAppointmentId()),
                new CustomerId(entity.getCustomerId()), new ServiceId(entity.getServiceId()), entity.getKind(),
                entity.getStartsAt(), entity.getSendAt(), entity.getStatus(), entity.getReason(),
                entity.getCancelReason(), entity.getMessageId() == null ? null : new MessageId(entity.getMessageId()));
    }

    public static ScheduledMessagePersistenceEntity toPersistenceFromDomain(ScheduledMessage message) {
        var entity = new ScheduledMessagePersistenceEntity();
        entity.setId(message.getId() == null ? null : message.getId().value());
        entity.setOrganizationId(message.getOrganizationId().value());
        entity.setEstablishmentId(message.getEstablishmentId().value());
        entity.setAppointmentId(message.getAppointmentId().value());
        entity.setCustomerId(message.getCustomerId().value());
        entity.setServiceId(message.getServiceId().value());
        entity.setKind(message.getKind());
        entity.setStartsAt(message.getStartsAt());
        entity.setSendAt(message.getSendAt());
        entity.setStatus(message.getStatus());
        entity.setReason(message.getReason());
        entity.setCancelReason(message.getCancelReason());
        entity.setMessageId(message.getMessageId() == null ? null : message.getMessageId().value());
        return entity;
    }
}
