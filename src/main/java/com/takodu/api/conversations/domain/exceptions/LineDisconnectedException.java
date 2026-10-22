package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** W15: nothing is sent through a line that is not connected. */
public class LineDisconnectedException extends BusinessRuleViolationException {
    public static final String MESSAGE = "The WhatsApp line is disconnected; the owner connects it again";

    public LineDisconnectedException() {
        super("LINE_DISCONNECTED", MESSAGE);
    }
}
