package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The free times (B16) the assistant offers at once, and how many more there are, so a full day never hides its
 * afternoon behind the first few morning times.
 *
 * @param shown the offered times, earliest first
 * @param more how many free times were left out
 */
public record TimeOffer(List<LocalTime> shown, int more) {
    public TimeOffer {
        shown = List.copyOf(shown);
        if (more < 0) throw new IllegalArgumentException("more must not be negative");
    }

    /** @param times every free time of the day, earliest first; at most {@code max} are kept, spread across the day */
    public static TimeOffer spread(List<LocalTime> times, int max) {
        requirePositive(max);
        if (times.size() <= max) return new TimeOffer(times, 0);
        if (max == 1) return new TimeOffer(List.of(times.getFirst()), times.size() - 1);
        var last = times.size() - 1;
        var shown = IntStream.range(0, max).mapToObj(i -> times.get(i * last / (max - 1))).toList();
        return new TimeOffer(shown, times.size() - max);
    }

    /** @return the {@code max} free times nearest to {@code around}, earliest first */
    public static TimeOffer closestTo(List<LocalTime> times, LocalTime around, int max) {
        requirePositive(max);
        var shown = times.stream()
                .sorted(Comparator.comparing((LocalTime time) -> Duration.between(around, time).abs())
                        .thenComparing(Comparator.naturalOrder()))
                .limit(max).sorted().toList();
        return new TimeOffer(shown, times.size() - shown.size());
    }

    private static void requirePositive(int max) {
        if (max < 1) throw new IllegalArgumentException("max must be positive");
    }
}
