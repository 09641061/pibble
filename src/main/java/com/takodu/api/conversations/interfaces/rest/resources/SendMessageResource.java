package com.takodu.api.conversations.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SendMessageRequest", description = "A staff reply inside the 24-hour window (W6)")
public record SendMessageResource(
        @NotBlank @Size(max = 4096) @Schema(examples = "Sí, mañana a las 4 está libre") String body) { }
