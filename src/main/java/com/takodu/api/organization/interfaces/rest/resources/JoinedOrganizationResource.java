package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "JoinedOrganization", description = "An organization of the caller, with their role in it")
public record JoinedOrganizationResource(UUID id, String name, Instant planActiveUntil, UUID memberId,
                                         @Schema(enumeration = {"owner", "manager", "staff"}) String role) { }
