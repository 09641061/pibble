package com.takodu.api.booking.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "CreateTimeBlockRequest", description = "A range in which a member, or everyone, cannot be booked (B15)")
public record CreateTimeBlockResource(
        @Schema(description = "The blocked member; leave out to block the whole establishment") UUID staffMemberId,
        @NotNull @Schema(examples = "2026-11-02T18:00:00Z") Instant start,
        @NotNull @Schema(examples = "2026-11-02T20:00:00Z") Instant end,
        @Size(max = 500) @Schema(examples = "Cita médica") String reason) { }
