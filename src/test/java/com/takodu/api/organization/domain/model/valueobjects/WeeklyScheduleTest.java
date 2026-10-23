package com.takodu.api.organization.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyScheduleTest {

    @Test
    void splitShiftIsSortedByOpeningTime() {
        var schedule = new WeeklySchedule(List.of(range(DayOfWeek.MONDAY, "15:00", "20:00"), range(DayOfWeek.MONDAY, "09:00", "13:00")));

        assertEquals(LocalTime.of(9, 0), schedule.on(DayOfWeek.MONDAY).getFirst().opensAt());
        assertTrue(schedule.on(DayOfWeek.SUNDAY).isEmpty());
    }

    @Test
    void overlappingRangesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new WeeklySchedule(List.of(
                range(DayOfWeek.FRIDAY, "09:00", "14:00"), range(DayOfWeek.FRIDAY, "13:00", "18:00"))));
    }

    @Test
    void adjacentRangesAreAllowed() {
        var schedule = new WeeklySchedule(List.of(range(DayOfWeek.FRIDAY, "09:00", "13:00"), range(DayOfWeek.FRIDAY, "13:00", "18:00")));

        assertEquals(2, schedule.on(DayOfWeek.FRIDAY).size());
    }

    @Test
    void rangeCrossingMidnightIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> range(DayOfWeek.SATURDAY, "18:00", "00:00"));
        assertThrows(IllegalArgumentException.class, () -> range(DayOfWeek.SATURDAY, "22:00", "02:00"));
    }

    @Test
    void secondsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> range(DayOfWeek.SATURDAY, "09:00:30", "12:00"));
    }

    private static OpeningRange range(DayOfWeek day, String opensAt, String closesAt) {
        return new OpeningRange(day, LocalTime.parse(opensAt), LocalTime.parse(closesAt));
    }
}
