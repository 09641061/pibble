package com.takodu.api.assistant.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "DailyUsage")
public record DailyUsageResource(@Schema(description = "Day in the establishment's time zone") LocalDate date,
                                 UsageTotalsResource totals) { }
