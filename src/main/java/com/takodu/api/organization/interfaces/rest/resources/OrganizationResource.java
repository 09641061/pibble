package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "Organization")
public record OrganizationResource(UUID id, String name,
                                   @Schema(enumeration = {"standard", "max"}) String plan,
                                   @Schema(description = "The plan is active until this instant (O5)") Instant planActiveUntil) { }
