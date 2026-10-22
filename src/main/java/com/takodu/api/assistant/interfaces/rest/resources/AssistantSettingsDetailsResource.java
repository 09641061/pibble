package com.takodu.api.assistant.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AssistantSettingsRequest", description = "Whether the assistant answers customers, and what it knows")
public record AssistantSettingsDetailsResource(
        @NotNull Boolean enabled,
        @Size(max = 4000) @Schema(description = "Address details, payment methods, policies, FAQs (A1)",
                examples = "Aceptamos Yape y Plin. Estacionamiento gratis en la puerta.") String businessInfo) { }
