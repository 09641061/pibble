package com.takodu.api.shared.infrastructure.time;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
class TestClockTest {

    private static final Instant NOON = Instant.parse("2026-10-21T17:00:00Z");

    @Inject
    Clock clock;

    @Inject
    TestClock testClock;

    @AfterEach
    void resetClock() {
        testClock.reset();
    }

    @Test
    void injectedClockIsTheTestClock() {
        assertSame(testClock, clock);
    }

    @Test
    void testsCanFixAndMoveTime() {
        testClock.set(NOON);
        testClock.advance(Duration.ofHours(12));

        assertEquals(NOON.plus(Duration.ofHours(12)), clock.instant());
    }
}
