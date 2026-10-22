package com.takodu.api.assistant.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** Two workers claimed turns of one chat at once; the loser's claim rolls back and its turn waits. */
public class TurnAlreadyRunningException extends ConflictException {
    public TurnAlreadyRunningException() {
        super("Assistant turn", "Another turn of this conversation is running");
    }
}
