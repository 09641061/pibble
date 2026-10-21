package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** W1: an establishment has one connected line, and a number belongs to one line. */
public class LineAlreadyConnectedException extends ConflictException {
    public LineAlreadyConnectedException(String message) { super("Line", "already connected", message); }
}
