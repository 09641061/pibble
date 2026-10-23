package com.takodu.api.conversations.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/** @param template {@code reminder} or {@code confirmation} */
@Schema(name = "SendTemplateRequest",
        description = "An appointment's reminder or confirmation, which reaches a customer whose window is closed (W6)")
public record SendTemplateResource(
        @NotNull @Pattern(regexp = "reminder|confirmation") @Schema(examples = "reminder") String template,
        @NotNull UUID appointmentId) { }
