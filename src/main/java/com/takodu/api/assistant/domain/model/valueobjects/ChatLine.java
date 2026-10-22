package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * One message of the chat, as the assistant remembers it.
 *
 * @param text the text, or the kind in brackets, such as {@code [audio]}
 */
public record ChatLine(ChatRole role, String text, Instant at) {
    public ChatLine {
        Objects.requireNonNull(role, "role is required");
        Objects.requireNonNull(text, "text is required");
        Objects.requireNonNull(at, "at is required");
    }

    /** Who wrote a message: the customer, the assistant (or Takodu's notifications), or a person of the business. */
    public enum ChatRole { CUSTOMER, ASSISTANT, STAFF }
}
