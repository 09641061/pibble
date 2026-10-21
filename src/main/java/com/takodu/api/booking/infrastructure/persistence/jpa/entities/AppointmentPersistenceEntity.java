package com.takodu.api.booking.infrastructure.persistence.jpa.entities;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.booking.infrastructure.persistence.jpa.converters.AppointmentStatusPersistenceConverter;
import com.takodu.api.booking.infrastructure.persistence.jpa.converters.BookingSourcePersistenceConverter;
import com.takodu.api.booking.infrastructure.persistence.jpa.converters.CancelledByPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
public class AppointmentPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "staff_member_id", nullable = false)
    private UUID staffMemberId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "price_cents", nullable = false, updatable = false)
    private int priceCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Convert(converter = AppointmentStatusPersistenceConverter.class)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Convert(converter = BookingSourcePersistenceConverter.class)
    @Column(name = "source", nullable = false, updatable = false)
    private BookingSource source;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Convert(converter = CancelledByPersistenceConverter.class)
    @Column(name = "cancelled_by")
    private CancelledBy cancelledBy;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;

    @Column(name = "last_idempotency_key")
    private UUID lastIdempotencyKey;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
