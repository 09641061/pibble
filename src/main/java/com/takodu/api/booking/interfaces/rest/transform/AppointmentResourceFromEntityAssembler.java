package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.interfaces.rest.resources.AppointmentResource;

import java.util.Locale;

public final class AppointmentResourceFromEntityAssembler {
    private AppointmentResourceFromEntityAssembler() { }

    public static AppointmentResource toResourceFromEntity(Appointment appointment) {
        return new AppointmentResource(appointment.getId().value(), appointment.getEstablishmentId().value(),
                appointment.getCustomerId().value(), appointment.getServiceId().value(),
                appointment.getStaffMemberId().value(), appointment.getRange().start(), appointment.getRange().end(),
                appointment.getPrice().cents(), appointment.getPrice().currency(), lower(appointment.getStatus()),
                lower(appointment.getSource()),
                appointment.getCancelReason() == null ? null : appointment.getCancelReason().value(),
                lower(appointment.getCancelledBy()));
    }

    private static String lower(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }
}
