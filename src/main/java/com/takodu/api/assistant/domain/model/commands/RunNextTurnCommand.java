package com.takodu.api.assistant.domain.model.commands;

import java.time.Instant;

public record RunNextTurnCommand(Instant now) {
    public RunNextTurnCommand {
        if (now == null) throw new IllegalArgumentException("now is required");
    }
}
