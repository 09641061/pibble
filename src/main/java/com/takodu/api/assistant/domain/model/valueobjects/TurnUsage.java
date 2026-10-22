package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.Objects;

/**
 * A9: what one turn cost, to measure cost per establishment.
 *
 * @param model the model that answered, such as {@code deepseek-chat}
 * @param cachedTokens input tokens the provider served from its prompt cache, cheaper; 0 when it does not report them
 */
public record TurnUsage(String model, int inputTokens, int cachedTokens, int outputTokens) {
    public TurnUsage {
        Objects.requireNonNull(model, "model is required");
        if (inputTokens < 0 || cachedTokens < 0 || outputTokens < 0)
            throw new IllegalArgumentException("Token counts are never negative");
    }
}
