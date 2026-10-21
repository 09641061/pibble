package com.takodu.api.shared.infrastructure.time;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockProducerTest {

    @Test
    void producesTheSystemClockInUtc() {
        assertEquals(ZoneOffset.UTC, new ClockProducer().clock().getZone());
    }
}
