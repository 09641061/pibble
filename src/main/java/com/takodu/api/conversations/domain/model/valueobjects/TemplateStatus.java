package com.takodu.api.conversations.domain.model.valueobjects;

/** Meta's review of a template on one WhatsApp account; only an approved template can be sent. */
public enum TemplateStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Meta stopped it for a while after customers reported or blocked it. */
    PAUSED,
    /** Meta disabled or deleted it; it will not come back by itself. */
    DISABLED
}
