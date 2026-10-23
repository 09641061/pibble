package com.takodu.api.conversations.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

/** The media job's request to download the media whose download is due first. */
public record DownloadNextMediaCommand(Instant now) {
    public DownloadNextMediaCommand {
        Objects.requireNonNull(now, "now is required");
    }
}
