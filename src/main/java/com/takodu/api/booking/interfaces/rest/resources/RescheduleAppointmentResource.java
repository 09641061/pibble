package com.takodu.api.booking.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "RescheduleAppointmentRequest", description = "A new start and optionally a new member; the length stays (B13)")
public record RescheduleAppointmentResource(
        @Schema(description = "Leave out to keep the current member") UUID staffMemberId,
        @NotNull @Schema(examples = "2026-11-03T16:00:00Z") Instant start) { }
