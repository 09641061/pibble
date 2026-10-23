package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalTime;

@Schema(name = "OpeningRange", description = "A range of one weekday, in the establishment's time zone")
public record OpeningRangeResource(
        @NotNull @Min(1) @Max(7) @Schema(description = "ISO weekday: 1 is Monday, 7 is Sunday", examples = "1") Integer weekday,
        @NotNull @Schema(type = SchemaType.STRING, examples = "09:00") LocalTime opensAt,
        @NotNull @Schema(type = SchemaType.STRING, examples = "13:00") LocalTime closesAt) { }
