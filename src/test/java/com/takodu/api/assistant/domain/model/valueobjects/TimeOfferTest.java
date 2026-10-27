package com.takodu.api.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeOfferTest {
    /** 09:00 to 17:00 every 15 minutes: 33 times. */
    private static final List<LocalTime> DAY = IntStream.rangeClosed(0, 32)
            .mapToObj(i -> LocalTime.of(9, 0).plusMinutes(15L * i)).toList();

    @Test
    void fewTimesAreAllShown() {
        var offer = TimeOffer.spread(DAY.subList(0, 4), 6);
        assertEquals(DAY.subList(0, 4), offer.shown());
        assertEquals(0, offer.more());
    }

    @Test
    void aFullDayIsSpreadFromFirstToLastTime() {
        var offer = TimeOffer.spread(DAY, 6);
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(12, 0), LocalTime.of(13, 45),
                LocalTime.of(15, 15), LocalTime.of(17, 0)), offer.shown());
        assertEquals(27, offer.more());
    }

    @Test
    void nearestTimesComeInOrder() {
        var offer = TimeOffer.closestTo(DAY, LocalTime.of(15, 10), 3);
        assertEquals(List.of(LocalTime.of(15, 0), LocalTime.of(15, 15), LocalTime.of(15, 30)), offer.shown());
        assertEquals(30, offer.more());
    }

    @Test
    void partsOfTheDaySplitAtNoonAndSix() {
        assertTrue(PartOfDay.MORNING.contains(LocalTime.of(11, 45)));
        assertFalse(PartOfDay.MORNING.contains(LocalTime.NOON));
        assertTrue(PartOfDay.AFTERNOON.contains(LocalTime.NOON));
        assertFalse(PartOfDay.AFTERNOON.contains(LocalTime.of(18, 0)));
        assertTrue(PartOfDay.EVENING.contains(LocalTime.of(18, 0)));
    }
}
