package com.takodu.api.assistant.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "AssistantUsage", description = "An establishment's assistant usage, day by day in its time zone (A9)")
public record AssistantUsageResource(UUID establishmentId, String establishmentName, String timeZone, LocalDate from,
                                     LocalDate to, UsageTotalsResource totals,
                                     @Schema(description = "Only days with answered turns, oldest first") List<DailyUsageResource> days) { }
