package com.takodu.api.conversations.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

/** @param now the dispatcher's time; notifications due by then are taken one at a time */
public record DispatchNextDueNotificationCommand(Instant now) {
    public DispatchNextDueNotificationCommand {
        Objects.requireNonNull(now, "now is required");
    }
}
