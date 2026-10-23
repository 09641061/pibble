package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.exceptions.ConflictException;

/** Someone else changed the appointment between this request's read and its write; reload and try again. */
public class AppointmentChangedException extends ConflictException {
    public AppointmentChangedException(AppointmentId appointmentId) {
        super("Appointment", "The appointment changed meanwhile; reload it: " + appointmentId.value());
    }
}
