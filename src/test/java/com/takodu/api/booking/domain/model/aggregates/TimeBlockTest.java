package com.takodu.api.booking.domain.model.aggregates;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeBlockTest {
    private static final Instant TEN = Instant.parse("2026-11-02T15:00:00Z");
    private static final TimeRange TEN_TO_TWELVE = TimeRange.of(TEN, Duration.ofHours(2));

    private final MemberId lucia = new MemberId(UUID.randomUUID());
    private final MemberId pepe = new MemberId(UUID.randomUUID());

    @Test
    void memberBlockKeepsOnlyThatMemberOut() {
        var block = create(lucia, TEN_TO_TWELVE, null);

        assertTrue(block.blocks(lucia, TimeRange.of(TEN.plus(Duration.ofMinutes(90)), Duration.ofHours(1))));
        assertFalse(block.blocks(pepe, TEN_TO_TWELVE));
    }

    @Test
    void wholeEstablishmentBlockKeepsEveryoneOut() {
        var block = create(null, TEN_TO_TWELVE, "Feriado");

        assertTrue(block.coversWholeEstablishment());
        assertTrue(block.blocks(pepe, TEN_TO_TWELVE));
    }

    @Test
    void appointmentEndingWhenTheBlockStartsIsNotBlocked() {
        var block = create(lucia, TEN_TO_TWELVE, null);

        assertFalse(block.blocks(lucia, TimeRange.of(TEN.minus(Duration.ofHours(1)), Duration.ofHours(1))));
    }

    @Test
    void blankReasonIsDropped() {
        assertNull(create(lucia, TEN_TO_TWELVE, "  ").getReason());
    }

    @Test
    void blockLastsAtMostAYear() {
        assertThrows(IllegalArgumentException.class, () -> create(lucia, TimeRange.of(TEN, Duration.ofDays(367)), null));
    }

    @Test
    void reasonHasAtMost500Characters() {
        assertThrows(IllegalArgumentException.class, () -> create(lucia, TEN_TO_TWELVE, "x".repeat(501)));
    }

    private static TimeBlock create(MemberId member, TimeRange range, String reason) {
        return TimeBlock.create(new OrganizationId(UUID.randomUUID()), new EstablishmentId(UUID.randomUUID()), member,
                range, reason, new IdempotencyKey(UUID.randomUUID()));
    }
}
