package com.takodu.api.assistant.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UsageTotals", description = "Answered turns and what they used (A9); no prices")
public record UsageTotalsResource(int turns, long inputTokens,
                                  @Schema(description = "Part of inputTokens served from the provider's cache") long cachedTokens,
                                  long outputTokens, int toolCalls) { }
