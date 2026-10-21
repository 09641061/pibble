package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** W2: Meta refused a step of Embedded Signup, such as an expired or already used code. The owner starts again. */
public class WhatsappSignupFailedException extends BusinessRuleViolationException {
    public WhatsappSignupFailedException(String message) {
        super("WHATSAPP_SIGNUP_FAILED", message);
    }
}
