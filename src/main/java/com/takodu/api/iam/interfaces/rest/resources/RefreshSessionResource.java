package com.takodu.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RefreshSessionRequest", description = "The refresh token from the last sign-in or refresh")
public record RefreshSessionResource(@NotBlank @Size(max = 100) String refreshToken) { }
