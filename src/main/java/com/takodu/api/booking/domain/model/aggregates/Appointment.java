package com.takodu.api.booking.domain.model.aggregates;

import com.takodu.api.booking.domain.exceptions.InvalidAppointmentTransitionException;
import com.takodu.api.booking.domain.exceptions.NoShowBeforeStartException;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * A customer receiving one service from one member at one establishment in one time range. The rules that need
 * other aggregates (B1-B7, B9) are checked by {@code AvailabilityPolicy} before the appointment is booked or moved.
 */
@Getter
public class Appointment {
    private final AppointmentId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private final CustomerId customerId;
    private final ServiceId serviceId;
    private MemberId staffMemberId;
    private TimeRange range;
    private final Money price;
    private AppointmentStatus status;
    private final BookingSource source;
    private final UserId createdBy;
    private CancelReason cancelReason;
    private CancelledBy cancelledBy;
    private final IdempotencyKey idempotencyKey;
    private IdempotencyKey lastIdempotencyKey;
    private final int version;

    /**
     * B8: the price and currency are copied from the service now and never change afterwards.
     *
     * @param createdBy the user who booked; required for staff bookings, null for the assistant
     */
    public static Appointment book(OrganizationId organizationId, CustomerId customerId, ServiceOffer service,
                                   MemberId staffMemberId, Instant start, BookingSource source, UserId createdBy,
                                   IdempotencyKey idempotencyKey) {
        if (source == BookingSource.STAFF && createdBy == null)
            throw new IllegalArgumentException("Staff bookings record who created them");
        return new Appointment(null, organizationId, service.establishmentId(), customerId, service.id(),
                staffMemberId, TimeRange.of(start, service.duration()), service.price(), AppointmentStatus.CONFIRMED,
                source, createdBy, null, null, idempotencyKey, null, 0);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Appointment(AppointmentId id, OrganizationId organizationId, EstablishmentId establishmentId,
                       CustomerId customerId, ServiceId serviceId, MemberId staffMemberId, TimeRange range, Money price,
                       AppointmentStatus status, BookingSource source, UserId createdBy, CancelReason cancelReason,
                       CancelledBy cancelledBy, IdempotencyKey idempotencyKey, IdempotencyKey lastIdempotencyKey,
                       int version) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId is required");
        this.staffMemberId = Objects.requireNonNull(staffMemberId, "staffMemberId is required");
        this.range = Objects.requireNonNull(range, "range is required");
        this.price = Objects.requireNonNull(price, "price is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.source = Objects.requireNonNull(source, "source is required");
        this.createdBy = createdBy;
        this.cancelReason = cancelReason;
        this.cancelledBy = cancelledBy;
        this.idempotencyKey = idempotencyKey;
        this.lastIdempotencyKey = lastIdempotencyKey;
        this.version = version;
    }

    /**
     * B13: moves a confirmed appointment to another start and possibly another member, keeping its length. The caller
     * re-runs {@code AvailabilityPolicy} on the new range.
     *
     * @throws InvalidAppointmentTransitionException when the appointment is not confirmed
     */
    public void reschedule(MemberId staffMemberId, Instant start, IdempotencyKey idempotencyKey) {
        if (status != AppointmentStatus.CONFIRMED)
            throw new InvalidAppointmentTransitionException("Only confirmed appointments can be rescheduled; this one is "
                    + lower(status));
        this.staffMemberId = Objects.requireNonNull(staffMemberId, "staffMemberId is required");
        this.range = TimeRange.of(start, range.length());
        this.lastIdempotencyKey = idempotencyKey;
    }

    /**
     * B12: cancels a confirmed or in-progress appointment, which frees its time at once.
     *
     * @throws InvalidAppointmentTransitionException when the appointment is already final (B10)
     */
    public void cancel(CancelReason reason, CancelledBy cancelledBy, IdempotencyKey idempotencyKey) {
        moveTo(AppointmentStatus.CANCELLED);
        this.cancelReason = Objects.requireNonNull(reason, "reason is required");
        this.cancelledBy = Objects.requireNonNull(cancelledBy, "cancelledBy is required");
        this.lastIdempotencyKey = idempotencyKey;
    }

    /**
     * B10, B11: starts, completes or marks a no-show. Cancelling goes through {@link #cancel}, which needs a reason.
     *
     * @param next {@code IN_PROGRESS}, {@code COMPLETED} or {@code NO_SHOW}
     * @throws InvalidAppointmentTransitionException when B10 does not allow the move
     * @throws NoShowBeforeStartException when marking a no-show before the start (B11)
     */
    public void changeStatus(AppointmentStatus next, Instant now, IdempotencyKey idempotencyKey) {
        if (next == AppointmentStatus.CANCELLED)
            throw new IllegalArgumentException("Cancel through cancellations, with a reason");
        if (next == AppointmentStatus.NO_SHOW && status.canMoveTo(next) && now.isBefore(range.start()))
            throw new NoShowBeforeStartException();
        moveTo(next);
        this.lastIdempotencyKey = idempotencyKey;
    }

    /** @return whether a transition with this key was already applied, so a retry returns the current state */
    public boolean alreadyApplied(IdempotencyKey idempotencyKey) {
        return idempotencyKey.equals(lastIdempotencyKey);
    }

    /** B5, B6: whether the appointment holds its member's and its customer's time. */
    public boolean occupiesTime() {
        return status.occupiesTime();
    }

    private void moveTo(AppointmentStatus next) {
        if (!status.canMoveTo(next))
            throw new InvalidAppointmentTransitionException("A %s appointment cannot become %s".formatted(lower(status),
                    lower(next)));
        this.status = next;
    }

    private static String lower(AppointmentStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
