package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "Establishment")
public record EstablishmentResource(UUID id, String name, String address,
                                    @Schema(examples = "America/Lima") String timeZone,
                                    @Schema(description = "Set when the establishment is archived") Instant archivedAt) { }
