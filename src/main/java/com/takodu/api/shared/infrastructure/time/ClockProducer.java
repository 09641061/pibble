package com.takodu.api.shared.infrastructure.time;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.time.Clock;

/**
 * The single source of the current time.
 *
 * <p>Services and jobs inject {@link Clock} and pass {@code clock.instant()} into domain methods; nothing calls
 * {@code Instant.now()}. Tests replace this bean with {@code TestClock}.</p>
 */
public class ClockProducer {

    /**
     * @return the system clock in UTC
     */
    @Produces
    @Singleton
    public Clock clock() {
        return Clock.systemUTC();
    }
}
