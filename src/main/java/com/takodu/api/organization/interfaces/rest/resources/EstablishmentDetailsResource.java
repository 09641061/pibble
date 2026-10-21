package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EstablishmentDetailsRequest", description = "Name, address and time zone of an establishment")
public record EstablishmentDetailsResource(
        @NotBlank @Size(max = 120) @Schema(examples = "Sede San Isidro") String name,
        @Size(max = 300) @Schema(examples = "Av. Conquistadores 120") String address,
        @Size(max = 64) @Schema(description = "IANA time zone; America/Lima when omitted", examples = "America/Lima") String timeZone) { }
