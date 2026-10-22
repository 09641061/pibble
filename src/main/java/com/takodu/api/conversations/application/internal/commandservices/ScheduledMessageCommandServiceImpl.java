package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.domain.model.aggregates.ScheduledMessage;
import com.takodu.api.conversations.domain.model.commands.CancelAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.MoveAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.ScheduleAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.conversations.domain.repositories.ScheduledMessageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ScheduledMessageCommandServiceImpl implements ScheduledMessageCommandService {
    private final ScheduledMessageRepository scheduledMessageRepository;
    private final Clock clock;

    public ScheduledMessageCommandServiceImpl(ScheduledMessageRepository scheduledMessageRepository, Clock clock) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.clock = clock;
    }

    /** W11, W12. A repeated event finds the rows and adds nothing. */
    @Override
    @Transactional
    public void handle(ScheduleAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        var now = clock.instant();
        if (kindOf(existing, NotificationKind.REMINDER).isEmpty()) {
            ScheduledMessage.reminderFor(command.organizationId(), command.establishmentId(), command.appointmentId(),
                    command.customerId(), command.serviceId(), command.startsAt(), now)
                    .ifPresent(scheduledMessageRepository::save);
        }
        if (command.bookedByStaff() && kindOf(existing, NotificationKind.CONFIRMATION).isEmpty()) {
            scheduledMessageRepository.save(ScheduledMessage.confirmationFor(command.organizationId(),
                    command.establishmentId(), command.appointmentId(), command.customerId(), command.serviceId(),
                    command.startsAt(), now));
        }
    }

    /** W13, decision 62. An appointment booked too close for a reminder gets one when it moves far enough. */
    @Override
    @Transactional
    public void handle(MoveAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        var now = clock.instant();
        for (var notification : existing) {
            notification.reschedule(command.startsAt(), now);
            scheduledMessageRepository.save(notification);
        }
        if (kindOf(existing, NotificationKind.REMINDER).isEmpty()) {
            ScheduledMessage.reminderFor(command.organizationId(), command.establishmentId(), command.appointmentId(),
                    command.customerId(), command.serviceId(), command.startsAt(), now)
                    .ifPresent(scheduledMessageRepository::save);
        }
    }

    /** W13, decision 63. */
    @Override
    @Transactional
    public void handle(CancelAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        for (var notification : existing) {
            notification.cancel("The appointment was cancelled");
            scheduledMessageRepository.save(notification);
        }
        var now = clock.instant();
        if (command.cancelledByStaff() && command.startsAt().isAfter(now)
                && kindOf(existing, NotificationKind.CANCELLATION).isEmpty()) {
            scheduledMessageRepository.save(ScheduledMessage.cancellationFor(command.organizationId(),
                    command.establishmentId(), command.appointmentId(), command.customerId(), command.serviceId(),
                    command.startsAt(), command.reason(), now));
        }
    }

    private static Optional<ScheduledMessage> kindOf(List<ScheduledMessage> notifications, NotificationKind kind) {
        return notifications.stream().filter(notification -> notification.getKind() == kind).findFirst();
    }
}
