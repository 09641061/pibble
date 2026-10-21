package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(name = "UpdateMemberRequest", description = "The whole editable state of a member")
public record UpdateMemberResource(
        @NotBlank @Size(max = 120) @Schema(examples = "Lucía") String displayName,
        @NotNull Boolean bookable,
        @NotBlank @Schema(description = "The owner's role stays owner", enumeration = {"owner", "manager", "staff"}) String role,
        @Size(max = 100) @Schema(description = "Where a staff member works; at least one for staff, ignored otherwise") Set<@NotNull UUID> establishmentIds) { }
