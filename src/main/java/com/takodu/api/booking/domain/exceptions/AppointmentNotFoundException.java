package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

public class AppointmentNotFoundException extends ResourceNotFoundException {
    public AppointmentNotFoundException(AppointmentId appointmentId) { super("Appointment", appointmentId.value()); }
}
