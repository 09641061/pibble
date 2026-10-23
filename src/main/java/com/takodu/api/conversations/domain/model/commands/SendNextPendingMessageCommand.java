package com.takodu.api.conversations.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

/** The sender job's request to deliver the oldest outbound message that is due. */
public record SendNextPendingMessageCommand(Instant now) {
    public SendNextPendingMessageCommand {
        Objects.requireNonNull(now, "now is required");
    }
}
