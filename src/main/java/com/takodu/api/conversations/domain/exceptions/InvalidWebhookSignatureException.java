package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;

/** The webhook was not signed with Takodu's app secret, so nothing in it is trusted or stored. */
public class InvalidWebhookSignatureException extends AuthenticationFailedException {
    public InvalidWebhookSignatureException() {
        super("WEBHOOK_SIGNATURE_INVALID", "The webhook signature is missing or wrong");
    }
}
