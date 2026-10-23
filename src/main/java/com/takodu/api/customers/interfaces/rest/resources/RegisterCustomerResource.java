package com.takodu.api.customers.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RegisterCustomerRequest", description = "Only the phone is required (C3, C4)")
public record RegisterCustomerResource(
        @NotBlank @Size(max = 30) @Schema(description = "With country code; a Peruvian mobile may omit +51",
                examples = "987 654 321") String phone,
        @Size(max = 120) @Schema(examples = "Rosa Quispe") String name,
        @Schema(description = "dni, ruc, ce or passport; together with documentNumber", examples = "dni") String documentType,
        @Size(max = 20) @Schema(examples = "45678912") String documentNumber,
        @Size(max = 2000) @Schema(examples = "Prefiere los sábados") String notes) { }
