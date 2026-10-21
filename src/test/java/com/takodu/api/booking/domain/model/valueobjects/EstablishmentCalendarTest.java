package com.takodu.api.booking.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstablishmentCalendarTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    /** Monday 09:00-13:00 and 15:00-20:00 in Lima; closed the rest of the week. */
    private final EstablishmentCalendar calendar = new EstablishmentCalendar(new EstablishmentId(UUID.randomUUID()), LIMA,
            List.of(new OpeningWindow(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(20, 0)),
                    new OpeningWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))),
            false, true);

    @Test
    void rangeInsideOneWindowIsOpen() {
        assertTrue(calendar.isOpenDuring(at("12:00", 60)));
        assertTrue(calendar.isOpenDuring(at("09:00", 240)));
    }

    @Test
    void rangeAcrossTheLunchBreakIsClosed() {
        assertFalse(calendar.isOpenDuring(at("12:30", 60)));
    }

    @Test
    void rangeEndingAfterClosingIsClosed() {
        assertFalse(calendar.isOpenDuring(at("19:30", 45)));
    }

    @Test
    void closedWeekdayIsClosed() {
        var tuesday = TimeRange.of(MONDAY.plusDays(1).atTime(10, 0).atZone(LIMA).toInstant(), Duration.ofHours(1));

        assertFalse(calendar.isOpenDuring(tuesday));
    }

    @Test
    void hoursAreReadInTheEstablishmentsTimeZone() {
        var tenInLimaIsThreeInUtc = TimeRange.of(Instant.parse("2026-11-02T15:00:00Z"), Duration.ofHours(1));

        assertTrue(calendar.isOpenDuring(tenInLimaIsThreeInUtc));
    }

    @Test
    void windowsOfADayComeEarliestFirst() {
        assertEquals(LocalTime.of(9, 0), calendar.windowsOn(MONDAY).getFirst().opensAt());
        assertTrue(calendar.windowsOn(MONDAY.plusDays(1)).isEmpty());
    }

    private static TimeRange at(String localTime, int minutes) {
        return TimeRange.of(MONDAY.atTime(LocalTime.parse(localTime)).atZone(LIMA).toInstant(), Duration.ofMinutes(minutes));
    }
}
