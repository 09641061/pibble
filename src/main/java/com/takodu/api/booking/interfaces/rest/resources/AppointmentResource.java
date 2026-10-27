package com.takodu.api.booking.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * @param priceCents the price when booked (B8)
 * @param cancelReason null unless cancelled
 * @param createdAt when it was booked, so the calendar can tell a new appointment from one already shown
 */
@Schema(name = "Appointment")
public record AppointmentResource(UUID id, UUID establishmentId, UUID customerId, UUID serviceId, UUID staffMemberId,
                                  Instant start, Instant end, long priceCents,
                                  @Schema(examples = "PEN") String currency,
                                  @Schema(enumeration = {"confirmed", "in_progress", "completed", "cancelled", "no_show"}) String status,
                                  @Schema(enumeration = {"staff", "assistant"}) String source,
                                  String cancelReason,
                                  @Schema(enumeration = {"staff", "customer"}) String cancelledBy,
                                  Instant createdAt) { }
