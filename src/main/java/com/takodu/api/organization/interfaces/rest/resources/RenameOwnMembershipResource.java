package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RenameOwnMembershipRequest", description = "The name the team sees for the caller in this organization")
public record RenameOwnMembershipResource(@NotBlank @Size(max = 120) @Schema(examples = "Lucía") String displayName) { }
