package com.takodu.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RequestLoginCodeRequest", description = "Where to email a sign-in code")
public record RequestLoginCodeResource(@NotBlank @Size(max = 254) @Schema(examples = "ana@example.com") String email) { }
