package com.takodu.api.iam.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "Session", description = "A device where the user is signed in")
public record SessionResource(UUID id, Instant lastUsedAt, Instant expiresAt,
                              @Schema(description = "Whether the request was made from this session") boolean current) { }
