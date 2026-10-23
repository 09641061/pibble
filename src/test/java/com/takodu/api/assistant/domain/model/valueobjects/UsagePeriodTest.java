package com.takodu.api.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsagePeriodTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Test
    void wholeLocalDaysBothIncluded() {
        var october = new UsagePeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertEquals(Instant.parse("2026-10-01T05:00:00Z"), october.start(LIMA));
        assertEquals(Instant.parse("2026-11-01T05:00:00Z"), october.end(LIMA));
        assertTrue(october.earliestStart().isBefore(october.start(LIMA)));
        assertTrue(october.latestEnd().isAfter(october.end(LIMA)));
    }

    @Test
    void aReversedOrLongerThanAYearPeriodIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new UsagePeriod(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01")));
        assertThrows(IllegalArgumentException.class,
                () -> new UsagePeriod(LocalDate.parse("2025-10-01"), LocalDate.parse("2026-10-02")));
    }
}
