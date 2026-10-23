package com.takodu.api.shared.infrastructure.time;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Replaces {@link ClockProducer} in every {@code @QuarkusTest}. It follows the system clock until a test fixes it
 * with {@link #set(Instant)}; a test that moves time calls {@link #reset()} afterwards.
 *
 * <p>A fixed instant keeps only microseconds, the precision PostgreSQL stores, so a test at an exact boundary
 * (such as 30 days after sign-in) compares the same instant the database holds.</p>
 */
@Alternative
@Priority(1)
@Singleton
public class TestClock extends Clock {

    private volatile Instant fixed;

    /** @param instant the instant every later {@link #instant()} returns */
    public void set(Instant instant) {
        this.fixed = instant.truncatedTo(ChronoUnit.MICROS);
    }

    /** @param duration how far to move the clock, from the fixed instant or from now */
    public void advance(Duration duration) {
        this.fixed = instant().plus(duration).truncatedTo(ChronoUnit.MICROS);
    }

    /** Goes back to following the system clock. */
    public void reset() {
        this.fixed = null;
    }

    @Override
    public Instant instant() {
        Instant current = fixed;
        return current != null ? current : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Pibble keeps instants in UTC");
    }
}
