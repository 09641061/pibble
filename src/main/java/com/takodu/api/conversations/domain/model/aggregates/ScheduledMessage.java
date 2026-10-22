package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationStatus;
import com.takodu.api.conversations.domain.model.valueobjects.ScheduledMessageId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * W11-W15: one notification about one appointment, waiting for its time. It keeps what it needs to be written and
 * re-checked, because conversations hears about appointments only through booking's events. When it is due, it
 * becomes a {@link Message} in the outbox, which retries and fails it (W14).
 */
@Getter
public class ScheduledMessage {
    /** W11: a reminder goes this long before the appointment. */
    public static final Duration REMINDER_LEAD = Duration.ofHours(24);
    /** Decision 61: an appointment booked less than 24 hours ahead is reminded this long before it. */
    public static final Duration LATE_REMINDER_LEAD = Duration.ofHours(2);
    /** W11, decision 60: an appointment booked less than this far ahead gets no reminder. */
    public static final Duration NO_REMINDER_WITHIN = Duration.ofHours(3);
    /** Decision 65: a notification held back goes out only if the appointment is more than this far away. */
    public static final Duration HELD_CUTOFF = Duration.ofHours(1);
    /** A held notification is checked again this often, so a reconnected line or a paid plan releases it. */
    public static final Duration HOLD_RECHECK = Duration.ofMinutes(10);

    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH);

    private final ScheduledMessageId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private final AppointmentId appointmentId;
    private final CustomerId customerId;
    private final ServiceId serviceId;
    private final NotificationKind kind;
    private Instant startsAt;
    private Instant sendAt;
    private NotificationStatus status;
    private String reason;
    private final String cancelReason;
    private MessageId messageId;

    /**
     * W11, decisions 60 and 61: 24 hours before; 2 hours before when booked less than 24 hours ahead; none when
     * booked less than 3 hours ahead.
     */
    public static Optional<ScheduledMessage> reminderFor(OrganizationId organizationId, EstablishmentId establishmentId,
                                                         AppointmentId appointmentId, CustomerId customerId,
                                                         ServiceId serviceId, Instant startsAt, Instant now) {
        return reminderTime(startsAt, now).map(sendAt -> new ScheduledMessage(null, organizationId, establishmentId,
                appointmentId, customerId, serviceId, NotificationKind.REMINDER, startsAt, sendAt,
                NotificationStatus.PENDING, null, null, null));
    }

    /** W12: staff booked it, so the customer hears about it now; the assistant's own reply confirms its bookings. */
    public static ScheduledMessage confirmationFor(OrganizationId organizationId, EstablishmentId establishmentId,
                                                   AppointmentId appointmentId, CustomerId customerId,
                                                   ServiceId serviceId, Instant startsAt, Instant now) {
        return new ScheduledMessage(null, organizationId, establishmentId, appointmentId, customerId, serviceId,
                NotificationKind.CONFIRMATION, startsAt, now, NotificationStatus.PENDING, null, null, null);
    }

    /**
     * W13, decision 63: staff cancelled, so the customer is told why; a customer who cancelled already knows.
     *
     * @param cancelReason what staff wrote, shown to the customer
     */
    public static ScheduledMessage cancellationFor(OrganizationId organizationId, EstablishmentId establishmentId,
                                                   AppointmentId appointmentId, CustomerId customerId,
                                                   ServiceId serviceId, Instant startsAt, String cancelReason,
                                                   Instant now) {
        if (cancelReason == null || cancelReason.isBlank())
            throw new IllegalArgumentException("A cancellation message needs the reason");
        return new ScheduledMessage(null, organizationId, establishmentId, appointmentId, customerId, serviceId,
                NotificationKind.CANCELLATION, startsAt, now, NotificationStatus.PENDING, null, cancelReason, null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public ScheduledMessage(ScheduledMessageId id, OrganizationId organizationId, EstablishmentId establishmentId,
                            AppointmentId appointmentId, CustomerId customerId, ServiceId serviceId,
                            NotificationKind kind, Instant startsAt, Instant sendAt, NotificationStatus status,
                            String reason, String cancelReason, MessageId messageId) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId is required");
        this.kind = Objects.requireNonNull(kind, "kind is required");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt is required");
        this.sendAt = Objects.requireNonNull(sendAt, "sendAt is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.reason = reason;
        this.cancelReason = cancelReason;
        this.messageId = messageId;
    }

    /**
     * W13, decision 62: the appointment moved. A reminder follows the booking rules from now: it moves, is cancelled
     * when the new time is less than 3 hours away, and is armed again when the old one already went out, was
     * cancelled or skipped, since that one was about another time. A pending confirmation tells the new time.
     */
    public void reschedule(Instant newStartsAt, Instant now) {
        if (kind == NotificationKind.REMINDER) {
            var time = reminderTime(newStartsAt, now);
            if (time.isEmpty()) {
                cancel("The appointment moved less than 3 hours ahead");
            } else {
                startsAt = newStartsAt;
                sendAt = time.get();
                status = NotificationStatus.PENDING;
                reason = null;
                messageId = null;
            }
        } else if (status == NotificationStatus.PENDING) {
            startsAt = newStartsAt;
        }
    }

    /** W13: the appointment was cancelled, or moved too close. Only a notification still waiting changes. */
    public void cancel(String why) {
        if (status != NotificationStatus.PENDING) return;
        status = NotificationStatus.CANCELLED;
        reason = why;
    }

    /** W15: the line, the plan or the template does not allow sending yet; it is checked again later. */
    public void hold(String why, Instant now) {
        if (status != NotificationStatus.PENDING) throw new IllegalStateException("Only a pending notification waits");
        reason = Objects.requireNonNull(why, "why is required");
        sendAt = now.plus(HOLD_RECHECK);
    }

    /**
     * @return why it is no use sending it now: the appointment started, or it was held and is now within an hour
     *         (decision 65); empty when it may go out
     */
    public Optional<String> lateness(Instant now) {
        if (!startsAt.isAfter(now)) return Optional.of("The appointment already started");
        if (isHeld() && !startsAt.isAfter(now.plus(HELD_CUTOFF)))
            return Optional.of("Held back until less than an hour before the appointment");
        return Optional.empty();
    }

    public void skip(String why) {
        if (status != NotificationStatus.PENDING) throw new IllegalStateException("Only a pending notification is skipped");
        status = NotificationStatus.SKIPPED;
        reason = Objects.requireNonNull(why, "why is required");
    }

    /** It is now a message in the outbox. */
    public void markSent(MessageId message) {
        if (status != NotificationStatus.PENDING) throw new IllegalStateException("Only a pending notification is sent");
        status = NotificationStatus.SENT;
        messageId = Objects.requireNonNull(message, "message is required");
        reason = null;
    }

    /** @return whether something kept it from going out at its time */
    public boolean isHeld() {
        return status == NotificationStatus.PENDING && reason != null;
    }

    /**
     * @param zone the establishment's time zone, in which the customer reads the date and time
     * @return the template's parameters, in Spanish: name, service, establishment, date, time; a cancellation
     *         takes name, service, date with time, and the reason
     */
    public List<String> parameters(String customerName, String serviceName, String establishmentName, ZoneId zone) {
        var local = startsAt.atZone(zone);
        var date = DATE.format(local);
        var time = TIME.format(local);
        return kind == NotificationKind.CANCELLATION
                ? List.of(customerName, serviceName, date + " a las " + time, cancelReason)
                : List.of(customerName, serviceName, establishmentName, date, time);
    }

    private static Optional<Instant> reminderTime(Instant startsAt, Instant now) {
        var ahead = Duration.between(now, startsAt);
        if (ahead.compareTo(NO_REMINDER_WITHIN) < 0) return Optional.empty();
        return Optional.of(ahead.compareTo(REMINDER_LEAD) >= 0 ? startsAt.minus(REMINDER_LEAD)
                : startsAt.minus(LATE_REMINDER_LEAD));
    }
}
