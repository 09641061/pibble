package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(name = "Service")
public record ServiceResource(UUID id, UUID establishmentId, String name, int durationMinutes, long priceCents,
                              @Schema(examples = "PEN") String currency, String instructions, boolean active,
                              Set<UUID> performerIds) { }
