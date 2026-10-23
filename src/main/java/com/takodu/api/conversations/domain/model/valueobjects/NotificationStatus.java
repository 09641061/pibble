package com.takodu.api.conversations.domain.model.valueobjects;

/** Where an appointment notification stands. Delivery, retries and failure are on the message it became (W14). */
public enum NotificationStatus {
    /** Waiting for its time, or held until the line, the plan or the template allows it. */
    PENDING,
    /** Handed to the outbox as a message. */
    SENT,
    /** The appointment was cancelled or moved too close to remind. */
    CANCELLED,
    /** It could not go out in time to be useful, or the customer is gone. */
    SKIPPED
}
