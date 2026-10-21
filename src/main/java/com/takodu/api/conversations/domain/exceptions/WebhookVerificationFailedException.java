package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;

/** The webhook handshake carried another verify token. */
public class WebhookVerificationFailedException extends AuthenticationFailedException {
    public WebhookVerificationFailedException() {
        super("WEBHOOK_VERIFICATION_FAILED", "The verify token does not match");
    }
}
