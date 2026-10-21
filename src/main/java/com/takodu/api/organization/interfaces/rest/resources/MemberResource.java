package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "Member", description = "An active member or a pending invitation")
public record MemberResource(UUID id, String email, String displayName,
                             @Schema(enumeration = {"owner", "manager", "staff"}) String role,
                             boolean bookable,
                             @Schema(enumeration = {"pending", "active"}) String status,
                             @Schema(description = "Assigned establishments of a staff member; empty for owners and managers") Set<UUID> establishmentIds,
                             @Schema(description = "When a pending invitation expires") Instant inviteExpiresAt,
                             Instant joinedAt) { }
