package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.Objects;

/**
 * What a message says or carries.
 *
 * @param body the text, the caption of media, the title a customer tapped, or the reaction emoji; null when there is none
 * @param mediaId Meta's id of an image, audio, video, document or sticker, downloaded later
 * @param mediaMime such as {@code image/jpeg}
 */
public record MessageContent(MessageKind kind, String body, String mediaId, String mediaMime) {
    public MessageContent {
        Objects.requireNonNull(kind, "kind is required");
        if (body != null && body.isBlank()) body = null;
    }

    public static MessageContent text(String body) {
        return new MessageContent(MessageKind.TEXT, body, null, null);
    }
}
