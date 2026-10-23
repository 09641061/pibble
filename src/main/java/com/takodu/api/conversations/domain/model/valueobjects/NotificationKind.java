package com.takodu.api.conversations.domain.model.valueobjects;

/** W11-W13: what an appointment notification tells the customer, and the template it uses outside the window. */
public enum NotificationKind {
    CONFIRMATION(NotificationTemplate.CONFIRMATION),
    REMINDER(NotificationTemplate.REMINDER),
    CANCELLATION(NotificationTemplate.CANCELLATION);

    private final NotificationTemplate template;

    NotificationKind(NotificationTemplate template) {
        this.template = template;
    }

    public NotificationTemplate template() {
        return template;
    }

    /**
     * W6, W11: a reminder always goes as its template, for its buttons; the others only when the window is closed,
     * and as the same text otherwise.
     */
    public boolean goesAsTemplate(boolean windowOpen) {
        return this == REMINDER || !windowOpen;
    }
}
