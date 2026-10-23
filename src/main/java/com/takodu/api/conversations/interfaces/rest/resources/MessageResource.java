package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param direction {@code in} or {@code out}
 * @param origin {@code customer}, {@code assistant}, {@code staff}, {@code business_app}, {@code system} or {@code history}
 * @param status {@code received}, {@code pending}, {@code sent}, {@code delivered}, {@code read} or {@code failed}
 * @param mediaStatus for media, {@code pending} while it downloads, {@code ready} to fetch from
 *        {@code /api/v1/media/{id}}, {@code failed}, or {@code removed} once the file was deleted (C5, retention);
 *        null without media
 * @param error why sending failed, such as Meta's {@code 131047: Re-engagement message}
 * @param sentByMemberId the member who wrote a staff reply
 * @param choicesStyle {@code buttons} or {@code list} for an assistant reply that offered choices; null otherwise
 * @param choices the titles the customer could tap, in order; empty when none
 */
@Schema(name = "Message")
public record MessageResource(UUID id, UUID conversationId, String direction, String origin, String kind, String body,
                              boolean hasMedia, String mediaMime, String mediaStatus, String templateName, String status, String error,
                              UUID sentByMemberId, String choicesStyle, List<String> choices, Instant occurredAt) { }
