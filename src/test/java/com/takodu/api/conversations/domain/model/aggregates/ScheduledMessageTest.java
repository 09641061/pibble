package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationStatus;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledMessageTest {
    private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");

    @Test
    void reminderGoesADayBeforeWhenBookedADayOrMoreAhead() {
        var start = NOW.plus(Duration.ofDays(3));

        assertEquals(start.minus(Duration.ofHours(24)), reminder(start).orElseThrow().getSendAt());
        assertEquals(NOW, reminder(NOW.plus(Duration.ofHours(24))).orElseThrow().getSendAt());
    }

    @Test
    void reminderGoesTwoHoursBeforeWhenBookedLessThanADayAhead() {
        var start = NOW.plus(Duration.ofHours(24)).minusSeconds(1);

        assertEquals(start.minus(Duration.ofHours(2)), reminder(start).orElseThrow().getSendAt());
        assertEquals(NOW.plus(Duration.ofHours(1)), reminder(NOW.plus(Duration.ofHours(3))).orElseThrow().getSendAt());
    }

    @Test
    void noReminderWhenBookedLessThanThreeHoursAhead() {
        assertTrue(reminder(NOW.plus(Duration.ofHours(3)).minusSeconds(1)).isEmpty());
        assertTrue(reminder(NOW.plus(Duration.ofHours(2))).isEmpty());
    }

    @Test
    void reschedulingMovesTheReminderOrCancelsItWhenTooClose() {
        var reminder = reminder(NOW.plus(Duration.ofDays(3))).orElseThrow();

        reminder.reschedule(NOW.plus(Duration.ofDays(5)), NOW);
        assertEquals(NOW.plus(Duration.ofDays(4)), reminder.getSendAt());

        reminder.reschedule(NOW.plus(Duration.ofHours(2)), NOW);
        assertEquals(NotificationStatus.CANCELLED, reminder.getStatus());
    }

    @Test
    void reminderThatWentOutIsArmedAgainForTheNewTime() {
        var reminder = reminder(NOW.plus(Duration.ofDays(1))).orElseThrow();
        reminder.markSent(new MessageId(UUID.randomUUID()));

        reminder.reschedule(NOW.plus(Duration.ofDays(4)), NOW);

        assertEquals(NotificationStatus.PENDING, reminder.getStatus());
        assertEquals(NOW.plus(Duration.ofDays(3)), reminder.getSendAt());
        assertEquals(null, reminder.getMessageId());
    }

    @Test
    void sentConfirmationIsNotSentAgainWhenTheAppointmentMoves() {
        var confirmation = confirmation(NOW.plus(Duration.ofDays(2)));
        confirmation.markSent(new MessageId(UUID.randomUUID()));

        confirmation.reschedule(NOW.plus(Duration.ofDays(4)), NOW);
        confirmation.cancel("The appointment was cancelled");

        assertEquals(NotificationStatus.SENT, confirmation.getStatus());
        assertEquals(NOW.plus(Duration.ofDays(2)), confirmation.getStartsAt());
    }

    @Test
    void heldNotificationGoesOutOnlyMoreThanAnHourBeforeTheAppointment() {
        var confirmation = confirmation(NOW.plus(Duration.ofHours(3)));
        assertEquals(Optional.empty(), confirmation.lateness(NOW.plus(Duration.ofHours(2).plusMinutes(30))));

        confirmation.hold("The WhatsApp line is disconnected", NOW);

        assertEquals(NOW.plus(ScheduledMessage.HOLD_RECHECK), confirmation.getSendAt());
        assertTrue(confirmation.isHeld());
        assertEquals(Optional.empty(), confirmation.lateness(NOW.plus(Duration.ofHours(2)).minusSeconds(1)));
        assertTrue(confirmation.lateness(NOW.plus(Duration.ofHours(2))).isPresent());
    }

    @Test
    void appointmentThatStartedIsTooLateForAnything() {
        var confirmation = confirmation(NOW.plus(Duration.ofMinutes(30)));

        assertTrue(confirmation.lateness(NOW.plus(Duration.ofMinutes(30))).isPresent());
        confirmation.skip("The appointment already started");
        assertThrows(IllegalStateException.class, () -> confirmation.markSent(new MessageId(UUID.randomUUID())));
    }

    @Test
    void parametersAreInSpanishAndInTheEstablishmentsTime() {
        var start = Instant.parse("2026-10-24T15:00:00Z");
        var lima = ZoneId.of("America/Lima");

        assertEquals(List.of("Ana", "Corte", "Sede Miraflores", "sábado 24 de octubre", "10:00"),
                confirmation(start).parameters("Ana", "Corte", "Sede Miraflores", lima));
        assertEquals(List.of("Ana", "Corte", "sábado 24 de octubre a las 10:00", "el local cerrará"),
                ScheduledMessage.cancellationFor(org(), est(), appointment(), customer(), service(), start,
                        "el local cerrará", NOW).parameters("Ana", "Corte", "Sede Miraflores", lima));
    }

    @Test
    void cancellationNeedsAReason() {
        assertThrows(IllegalArgumentException.class, () -> ScheduledMessage.cancellationFor(org(), est(),
                appointment(), customer(), service(), NOW.plus(Duration.ofDays(1)), " ", NOW));
        assertFalse(confirmation(NOW.plus(Duration.ofDays(1))).isHeld());
    }

    private static Optional<ScheduledMessage> reminder(Instant start) {
        return ScheduledMessage.reminderFor(org(), est(), appointment(), customer(), service(), start, NOW);
    }

    private static ScheduledMessage confirmation(Instant start) {
        return ScheduledMessage.confirmationFor(org(), est(), appointment(), customer(), service(), start, NOW);
    }

    private static OrganizationId org() { return new OrganizationId(UUID.randomUUID()); }

    private static EstablishmentId est() { return new EstablishmentId(UUID.randomUUID()); }

    private static AppointmentId appointment() { return new AppointmentId(UUID.randomUUID()); }

    private static CustomerId customer() { return new CustomerId(UUID.randomUUID()); }

    private static ServiceId service() { return new ServiceId(UUID.randomUUID()); }
}
