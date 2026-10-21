package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(name = "ServiceDetailsRequest", description = "The whole editable state of a service; prices are in PEN")
public record ServiceDetailsResource(
        @NotBlank @Size(max = 120) @Schema(examples = "Corte clásico") String name,
        @NotNull @Min(1) @Max(720) @Schema(examples = "45") Integer durationMinutes,
        @NotNull @Min(0) @Max(100_000_000) @Schema(description = "Price in céntimos", examples = "3500") Integer priceCents,
        @Size(max = 1000) @Schema(description = "Sent to the customer when booked", examples = "Llega 5 minutos antes") String instructions,
        @Size(max = 100) @Schema(description = "Members who perform it (O15)") Set<@NotNull UUID> performerIds) { }
