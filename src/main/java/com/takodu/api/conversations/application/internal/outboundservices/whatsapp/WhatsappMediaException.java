package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

/**
 * Meta did not hand over a media file. The text is stored on the message for staff, so it never holds the url, a
 * token or the content.
 */
public class WhatsappMediaException extends RuntimeException {
    private final boolean retryable;

    /** @param retryable whether trying again may succeed, as after a timeout, a 5xx or an expired url */
    public WhatsappMediaException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
