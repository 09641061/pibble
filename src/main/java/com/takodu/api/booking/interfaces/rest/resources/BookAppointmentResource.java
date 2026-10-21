package com.takodu.api.booking.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "BookAppointmentRequest", description = "The establishment is the service's (B1)")
public record BookAppointmentResource(
        @NotNull UUID customerId,
        @NotNull UUID serviceId,
        @Schema(description = "Leave out to let booking choose: the least busy member that day, then the first to join (B17)")
        UUID staffMemberId,
        @NotNull @Schema(examples = "2026-11-02T15:00:00Z") Instant start) { }
