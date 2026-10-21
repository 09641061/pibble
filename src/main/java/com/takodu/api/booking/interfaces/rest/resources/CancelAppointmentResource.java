package com.takodu.api.booking.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CancelAppointmentRequest", description = "B12")
public record CancelAppointmentResource(
        @NotBlank @Size(max = 500) @Schema(examples = "Se enfermó") String reason,
        @Schema(description = "Who asked to cancel; staff record a customer's call as customer. Defaults to staff.",
                enumeration = {"staff", "customer"}) String cancelledBy) { }
