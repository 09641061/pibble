package com.takodu.api.conversations.domain.model.valueobjects;

/** W10: the delivery state of a message. It only moves forward; {@code FAILED} can replace a status that is not final. */
public enum DeliveryStatus {
    /** An inbound message; Meta reports no status for it. */
    RECEIVED(0),
    /** Waiting in the outbox. */
    PENDING(1),
    SENT(2),
    DELIVERED(3),
    READ(4),
    FAILED(5);

    private final int rank;

    DeliveryStatus(int rank) {
        this.rank = rank;
    }

    /**
     * @param next the status Meta reported, possibly out of order
     * @return the status after applying it; a late {@code delivered} after {@code read} changes nothing
     */
    public DeliveryStatus advanceTo(DeliveryStatus next) {
        if (this == READ || this == FAILED || this == RECEIVED) return this;
        if (next == FAILED) return FAILED;
        return next.rank > rank ? next : this;
    }
}
