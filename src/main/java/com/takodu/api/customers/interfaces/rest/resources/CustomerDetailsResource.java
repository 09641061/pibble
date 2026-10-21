package com.takodu.api.customers.interfaces.rest.resources;

import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CustomerDetailsRequest", description = "The whole editable state of a customer; the phone never changes (C1, C4)")
public record CustomerDetailsResource(
        @Size(max = 120) @Schema(examples = "Rosa Quispe") String name,
        @Schema(description = "dni, ruc, ce or passport; together with documentNumber", examples = "dni") String documentType,
        @Size(max = 20) @Schema(examples = "45678912") String documentNumber,
        @Size(max = 2000) @Schema(examples = "Prefiere los sábados") String notes) { }
