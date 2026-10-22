package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * @param direction {@code in} or {@code out}
 * @param origin {@code customer}, {@code assistant}, {@code staff}, {@code business_app}, {@code system} or {@code history}
 * @param status {@code received}, {@code pending}, {@code sent}, {@code delivered}, {@code read} or {@code failed}
 * @param error why sending failed, such as Meta's {@code 131047: Re-engagement message}
 * @param sentByMemberId the member who wrote a staff reply
 */
@Schema(name = "Message")
public record MessageResource(UUID id, UUID conversationId, String direction, String origin, String kind, String body,
                              boolean hasMedia, String mediaMime, String templateName, String status, String error,
                              UUID sentByMemberId, Instant occurredAt) { }
