package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ChangePlan", description = "Send activeUntil or extendDays, not both; with neither only the plan changes")
public record ChangePlanResource(
        @Schema(enumeration = {"standard", "max"}, description = "Omit to keep the plan") @Pattern(regexp = "standard|max") String plan,
        @Schema(description = "New end of the paid period; a past instant ends the plan") Instant activeUntil,
        @Schema(description = "Days added to the current end, or to now once the plan has ended") @Min(1) @Max(3660) Integer extendDays,
        @Schema(description = "How and when the business paid, such as \"Yape 2026-10-23\"") @NotBlank @Size(max = 500) String note) { }
