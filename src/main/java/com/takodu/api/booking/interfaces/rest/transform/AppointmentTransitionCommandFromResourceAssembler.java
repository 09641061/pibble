package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.commands.CancelAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.ChangeAppointmentStatusCommand;
import com.takodu.api.booking.domain.model.commands.RescheduleAppointmentCommand;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.booking.interfaces.rest.resources.CancelAppointmentResource;
import com.takodu.api.booking.interfaces.rest.resources.ChangeAppointmentStatusResource;
import com.takodu.api.booking.interfaces.rest.resources.RescheduleAppointmentResource;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class AppointmentTransitionCommandFromResourceAssembler {
    private AppointmentTransitionCommandFromResourceAssembler() { }

    public static RescheduleAppointmentCommand toRescheduleCommandFromResource(
            OrganizationId organizationId, UUID appointmentId, UUID idempotencyKey, RescheduleAppointmentResource resource) {
        return new RescheduleAppointmentCommand(organizationId, new AppointmentId(appointmentId),
                resource.staffMemberId() == null ? null : new MemberId(resource.staffMemberId()), resource.start(),
                BookingSource.STAFF, new IdempotencyKey(idempotencyKey));
    }

    public static CancelAppointmentCommand toCancelCommandFromResource(
            OrganizationId organizationId, UUID appointmentId, UUID idempotencyKey, CancelAppointmentResource resource) {
        var cancelledBy = resource.cancelledBy() == null ? CancelledBy.STAFF
                : parse(CancelledBy.class, resource.cancelledBy(), "cancelledBy must be staff or customer");
        return new CancelAppointmentCommand(organizationId, new AppointmentId(appointmentId),
                new CancelReason(resource.reason()), cancelledBy, new IdempotencyKey(idempotencyKey));
    }

    public static ChangeAppointmentStatusCommand toChangeStatusCommandFromResource(
            OrganizationId organizationId, UUID appointmentId, UUID idempotencyKey,
            ChangeAppointmentStatusResource resource) {
        var status = parse(AppointmentStatus.class, resource.status(), "status must be in_progress, completed or no_show");
        return new ChangeAppointmentStatusCommand(organizationId, new AppointmentId(appointmentId), status,
                new IdempotencyKey(idempotencyKey));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String message) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().toLowerCase(Locale.ROOT).equals(value.strip()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }
}
