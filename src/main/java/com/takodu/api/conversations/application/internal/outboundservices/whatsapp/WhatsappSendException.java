package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

/** Meta did not accept a message. The text is stored on the message for staff, so it never holds the body or a token. */
public class WhatsappSendException extends RuntimeException {
    private final boolean retryable;

    /** @param retryable whether trying again may succeed, as after a timeout, a 5xx or Meta's rate limit */
    public WhatsappSendException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
