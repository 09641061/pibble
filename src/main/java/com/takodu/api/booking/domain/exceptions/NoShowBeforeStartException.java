package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B11 */
public class NoShowBeforeStartException extends BusinessRuleViolationException {
    public NoShowBeforeStartException() {
        super("NO_SHOW_BEFORE_START", "A customer can be marked as a no-show only after the start time");
    }
}
