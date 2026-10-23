package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** A staff template tells about one of the customer's confirmed appointments at this establishment that has not started. */
public class AppointmentNotUpcomingException extends BusinessRuleViolationException {
    public AppointmentNotUpcomingException(AppointmentId appointmentId) {
        super("APPOINTMENT_NOT_UPCOMING", "Appointment " + appointmentId.value()
                + " is not an upcoming confirmed appointment of this customer at this establishment");
    }
}
