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
}
