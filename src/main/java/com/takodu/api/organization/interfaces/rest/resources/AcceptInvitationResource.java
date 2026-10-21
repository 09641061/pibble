package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AcceptInvitationRequest")
public record AcceptInvitationResource(@NotBlank @Size(max = 100) @Schema(description = "The token from the invitation link") String token) { }
