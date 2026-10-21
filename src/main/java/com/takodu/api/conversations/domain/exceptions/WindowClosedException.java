package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** W6: more than 24 hours passed since the customer's last message, so only a template may be sent. */
public class WindowClosedException extends BusinessRuleViolationException {
    public static final String MESSAGE = "The customer last wrote more than 24 hours ago; send a template instead";

    public WindowClosedException() {
        super("WINDOW_CLOSED", MESSAGE);
    }
}
