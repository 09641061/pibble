package com.takodu.api.customers.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "Customer")
public record CustomerResource(UUID id, @Schema(examples = "+51987654321") String phone,
                               @Schema(description = "The typed name, else the WhatsApp profile name, else the phone")
                               String displayName,
                               String name, String whatsappProfileName,
                               @Schema(examples = "dni") String documentType, String documentNumber, String notes) { }
