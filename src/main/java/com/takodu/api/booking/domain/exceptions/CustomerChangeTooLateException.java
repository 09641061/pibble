package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B14: the assistant may not change an appointment less than 2 hours before it starts; a person must. */
public class CustomerChangeTooLateException extends BusinessRuleViolationException {
    public CustomerChangeTooLateException() {
        super("CUSTOMER_CHANGE_TOO_LATE", "The appointment starts in less than 2 hours; a person must change it");
    }
}
