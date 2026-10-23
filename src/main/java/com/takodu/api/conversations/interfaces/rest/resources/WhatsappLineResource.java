package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * @param mode {@code coexistence} or {@code cloud_api}
 * @param status {@code connected} or {@code disconnected}
 * @param templates each notification template the account has ({@code reminder}, {@code confirmation},
 *        {@code cancellation}) with Meta's review: {@code pending}, {@code approved}, {@code rejected}, {@code paused}
 *        or {@code disabled}; notifications wait for an approved one
 */
@Schema(name = "WhatsappLine")
public record WhatsappLineResource(UUID id, UUID establishmentId, String displayPhone, String mode, String status,
                                   Instant connectedAt, Map<String, String> templates) { }
