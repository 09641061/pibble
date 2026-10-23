package com.takodu.api.assistant.domain.model.valueobjects;

/**
 * A9: what answered turns used, summed. Prices stay outside the system, so this counts tokens and never money.
 *
 * @param turns answered turns; skipped and failed turns record no tokens
 * @param cachedTokens part of {@code inputTokens} the provider served from its prompt cache
 */
public record UsageTotals(int turns, long inputTokens, long cachedTokens, long outputTokens, int toolCalls) {
    public static final UsageTotals ZERO = new UsageTotals(0, 0, 0, 0, 0);

    public UsageTotals {
        if (turns < 0 || inputTokens < 0 || cachedTokens < 0 || outputTokens < 0 || toolCalls < 0)
            throw new IllegalArgumentException("Usage is never negative");
    }

    public UsageTotals plus(UsageTotals other) {
        return new UsageTotals(turns + other.turns, inputTokens + other.inputTokens, cachedTokens + other.cachedTokens,
                outputTokens + other.outputTokens, toolCalls + other.toolCalls);
    }
}
