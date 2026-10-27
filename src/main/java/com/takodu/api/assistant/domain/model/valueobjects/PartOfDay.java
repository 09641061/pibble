package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.LocalTime;

/** A part of the day a customer asks for, in the establishment's local time (O7). */
public enum PartOfDay {
    /** Before 12:00. */
    MORNING,
    /** From 12:00 to before 18:00. */
    AFTERNOON,
    /** From 18:00. */
    EVENING;

    private static final LocalTime NOON = LocalTime.NOON;
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    public boolean contains(LocalTime time) {
        return switch (this) {
            case MORNING -> time.isBefore(NOON);
            case AFTERNOON -> !time.isBefore(NOON) && time.isBefore(SIX_PM);
            case EVENING -> !time.isBefore(SIX_PM);
        };
    }
}
