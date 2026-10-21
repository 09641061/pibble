package com.takodu.api.booking.domain.model.valueobjects;

/** B10: the lifecycle of an appointment. {@code completed}, {@code cancelled} and {@code no_show} are final. */
public enum AppointmentStatus {
    CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW;

    /** B10 */
    public boolean canMoveTo(AppointmentStatus next) {
        return switch (this) {
            case CONFIRMED -> next == IN_PROGRESS || next == COMPLETED || next == NO_SHOW || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    /** B5, B6: whether the appointment holds its time; mirrors the database exclusion constraints. */
    public boolean occupiesTime() {
        return this == CONFIRMED || this == IN_PROGRESS;
    }
}
