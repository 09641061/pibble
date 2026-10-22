package com.takodu.api.conversations.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Where the previous page ended, for lists ordered newest first: the next page holds rows strictly older than
 * ({@code at}, {@code id}).
 */
public record PageCursor(Instant at, UUID id) {
    public PageCursor {
        Objects.requireNonNull(at, "at is required");
        Objects.requireNonNull(id, "id is required");
    }
}
