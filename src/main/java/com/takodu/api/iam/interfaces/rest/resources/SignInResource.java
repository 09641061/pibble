package com.takodu.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SignInRequest", description = "The email and the code it received")
public record SignInResource(
        @NotBlank @Size(max = 254) @Schema(examples = "ana@example.com") String email,
        @NotBlank @Schema(examples = "042917") String code) { }
