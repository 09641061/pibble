package com.takodu.api.conversations.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

/**
 * 07 §4: the retention job deletes one file.
 *
 * @param expiredBefore media that arrived before this instant has passed the retention period
 */
public record RemoveNextMediaCommand(Instant expiredBefore) {
    public RemoveNextMediaCommand {
        Objects.requireNonNull(expiredBefore, "expiredBefore is required");
    }
}
