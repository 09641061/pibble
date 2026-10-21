package com.takodu.api.booking.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** @param freeMemberIds who can take it; the first is who booking picks when no member is chosen (B17) */
@Schema(name = "Slot")
public record SlotResource(Instant start, Instant end, List<UUID> freeMemberIds) { }
