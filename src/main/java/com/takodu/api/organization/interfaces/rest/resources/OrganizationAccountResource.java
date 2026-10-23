package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "OrganizationAccount", description = "An organization as the Takodu team follows its plan")
public record OrganizationAccountResource(UUID id, String name,
                                          @Schema(enumeration = {"standard", "max"}) String plan,
                                          Instant planActiveUntil, boolean planActive, String ownerEmail,
                                          @Schema(description = "Compared with the plan's limit (O4)") int activeEstablishments) { }
