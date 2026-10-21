package com.takodu.api.booking.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** @param staffMemberId null when the block covers the whole establishment */
@Schema(name = "TimeBlock")
public record TimeBlockResource(UUID id, UUID establishmentId, UUID staffMemberId, Instant start, Instant end,
                                String reason) { }
