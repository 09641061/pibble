package com.takodu.api.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRangeTest {

    private static final Instant TEN = Instant.parse("2026-10-21T15:00:00Z");

    private static TimeRange minutes(long fromTen, long length) {
        return TimeRange.of(TEN.plus(Duration.ofMinutes(fromTen)), Duration.ofMinutes(length));
    }

    @Test
    void ofAddsTheLengthToTheStart() {
        var range = minutes(0, 45);

        assertEquals(TEN.plus(Duration.ofMinutes(45)), range.end());
        assertEquals(Duration.ofMinutes(45), range.length());
    }

    @Test
    void intersectingRangesOverlap() {
        assertTrue(minutes(0, 60).overlaps(minutes(30, 60)));
        assertTrue(minutes(30, 60).overlaps(minutes(0, 60)));
    }

    @Test
    void containedRangeOverlaps() {
        assertTrue(minutes(0, 60).overlaps(minutes(15, 15)));
    }

    @Test
    void backToBackRangesDoNotOverlap() {
        assertFalse(minutes(0, 30).overlaps(minutes(30, 30)));
        assertFalse(minutes(30, 30).overlaps(minutes(0, 30)));
    }

    @Test
    void emptyOrReversedRangeIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new TimeRange(TEN, TEN));

        assertEquals("end must be after start", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(TEN, TEN.minusSeconds(1)));
    }

    @Test
    void missingBoundIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(null, TEN));
        assertThrows(IllegalArgumentException.class, () -> TimeRange.of(TEN, null));
    }
}
