package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * @param mode {@code coexistence} or {@code cloud_api}
 * @param status {@code connected} or {@code disconnected}
 */
@Schema(name = "WhatsappLine")
public record WhatsappLineResource(UUID id, UUID establishmentId, String displayPhone, String mode, String status,
                                   Instant connectedAt) { }
