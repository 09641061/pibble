package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "TransferOwnershipRequest")
public record TransferOwnershipResource(@NotNull @Schema(description = "An active member of the organization") UUID newOwnerId) { }
