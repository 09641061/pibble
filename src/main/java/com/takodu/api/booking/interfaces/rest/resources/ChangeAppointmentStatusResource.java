package com.takodu.api.booking.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ChangeAppointmentStatusRequest", description = "B10, B11; cancelling goes through cancellations")
public record ChangeAppointmentStatusResource(
        @NotBlank @Schema(enumeration = {"in_progress", "completed", "no_show"}) String status) { }
