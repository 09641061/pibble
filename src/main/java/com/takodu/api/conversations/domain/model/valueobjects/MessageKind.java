package com.takodu.api.conversations.domain.model.valueobjects;

/** What a message contains; anything WhatsApp adds later is stored as {@code UNSUPPORTED}, never dropped. */
public enum MessageKind {
    TEXT, IMAGE, AUDIO, VIDEO, DOCUMENT, STICKER, LOCATION, INTERACTIVE, BUTTON, REACTION, TEMPLATE, UNSUPPORTED
}
