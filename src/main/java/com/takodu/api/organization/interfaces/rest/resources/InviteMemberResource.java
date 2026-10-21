package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(name = "InviteMemberRequest", description = "Who to invite and with which role")
public record InviteMemberResource(
        @NotBlank @Size(max = 254) @Schema(examples = "lucia@example.com") String email,
        @NotBlank @Size(max = 120) @Schema(examples = "Lucía") String displayName,
        @NotBlank @Schema(enumeration = {"manager", "staff"}) String role,
        @Size(max = 100) @Schema(description = "Where a staff member works; at least one for staff, ignored for managers") Set<@NotNull UUID> establishmentIds,
        @Schema(description = "Whether the member receives appointments; true when omitted") Boolean bookable) { }
