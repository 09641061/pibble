package com.takodu.api.booking.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.infrastructure.persistence.jpa.entities.AppointmentPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.UUID;

public final class AppointmentPersistenceAssembler {
    private AppointmentPersistenceAssembler() { }

    public static Appointment toDomainFromPersistence(AppointmentPersistenceEntity entity) {
        return new Appointment(new AppointmentId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), new CustomerId(entity.getCustomerId()),
                new ServiceId(entity.getServiceId()), new MemberId(entity.getStaffMemberId()),
                new TimeRange(entity.getStartsAt(), entity.getEndsAt()),
                new Money(entity.getPriceCents(), entity.getCurrency()), entity.getStatus(), entity.getSource(),
                entity.getCreatedBy() == null ? null : new UserId(entity.getCreatedBy()),
                entity.getCancelReason() == null ? null : new CancelReason(entity.getCancelReason()),
                entity.getCancelledBy(), key(entity.getIdempotencyKey()), key(entity.getLastIdempotencyKey()),
                entity.getVersion(), entity.getCreatedAt());
    }

    public static AppointmentPersistenceEntity toPersistenceFromDomain(Appointment appointment) {
        var entity = new AppointmentPersistenceEntity();
        entity.setId(appointment.getId() == null ? null : appointment.getId().value());
        entity.setOrganizationId(appointment.getOrganizationId().value());
        entity.setEstablishmentId(appointment.getEstablishmentId().value());
        entity.setCustomerId(appointment.getCustomerId().value());
        entity.setServiceId(appointment.getServiceId().value());
        entity.setStaffMemberId(appointment.getStaffMemberId().value());
        entity.setStartsAt(appointment.getRange().start());
        entity.setEndsAt(appointment.getRange().end());
        entity.setPriceCents(Math.toIntExact(appointment.getPrice().cents()));
        entity.setCurrency(appointment.getPrice().currency());
        entity.setStatus(appointment.getStatus());
        entity.setSource(appointment.getSource());
        entity.setCancelReason(appointment.getCancelReason() == null ? null : appointment.getCancelReason().value());
        entity.setCancelledBy(appointment.getCancelledBy());
        entity.setCreatedBy(appointment.getCreatedBy() == null ? null : appointment.getCreatedBy().value());
        entity.setIdempotencyKey(appointment.getIdempotencyKey() == null ? null : appointment.getIdempotencyKey().value());
        entity.setLastIdempotencyKey(appointment.getLastIdempotencyKey() == null ? null
                : appointment.getLastIdempotencyKey().value());
        entity.setVersion(appointment.getVersion());
        entity.setCreatedAt(appointment.getCreatedAt());
        return entity;
    }

    private static IdempotencyKey key(UUID value) {
        return value == null ? null : new IdempotencyKey(value);
    }
}
