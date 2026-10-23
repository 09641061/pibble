package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RenameOrganizationRequest")
public record RenameOrganizationResource(@NotBlank @Size(max = 120) @Schema(examples = "Barbería Don Pepe") String name) { }
