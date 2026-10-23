package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** W11-W13: Meta has not approved the template on this line yet, so it cannot be sent. */
public class TemplateNotApprovedException extends BusinessRuleViolationException {
    public TemplateNotApprovedException(String templateName) {
        super("TEMPLATE_NOT_APPROVED", "Meta has not approved the template " + templateName + " on this line yet");
    }
}
