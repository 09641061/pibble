package com.takodu.api.conversations.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ConnectWhatsappLineRequest", description = "What Embedded Signup returned to the browser (W2)")
public record ConnectWhatsappLineResource(
        @NotBlank @Schema(description = "Single-use code from Embedded Signup") String code,
        @NotBlank @Schema(examples = "102290129340398") String wabaId,
        @NotBlank @Schema(examples = "106540352242922") String phoneNumberId,
        @Pattern(regexp = "coexistence|cloud_api") @Schema(description = "Defaults to coexistence",
                enumeration = {"coexistence", "cloud_api"}) String mode,
        @Schema(description = "6-digit two-step verification PIN; cloud_api only") String pin) { }
