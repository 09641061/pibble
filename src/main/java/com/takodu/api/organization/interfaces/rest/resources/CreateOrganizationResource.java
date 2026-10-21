package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateOrganizationRequest", description = "The business, its first establishment and how the owner is shown to the team")
public record CreateOrganizationResource(
        @NotBlank @Size(max = 120) @Schema(examples = "Barbería Don Pepe") String name,
        @NotBlank @Size(max = 120) @Schema(examples = "Sede Miraflores") String establishmentName,
        @Size(max = 300) @Schema(examples = "Av. Larco 345, Miraflores") String address,
        @Size(max = 64) @Schema(description = "IANA time zone; America/Lima when omitted", examples = "America/Lima") String timeZone,
        @NotBlank @Size(max = 120) @Schema(examples = "Pepe") String ownerDisplayName) { }
