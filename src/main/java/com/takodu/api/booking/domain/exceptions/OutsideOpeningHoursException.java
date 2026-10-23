package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B3: the appointment must fit inside one opening range of its local day. */
public class OutsideOpeningHoursException extends BusinessRuleViolationException {
    public OutsideOpeningHoursException() {
        super("OUTSIDE_OPENING_HOURS", "The appointment does not fit inside the opening hours");
    }
}
