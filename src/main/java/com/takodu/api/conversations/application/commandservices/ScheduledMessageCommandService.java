package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.model.commands.CancelAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.DispatchNextDueNotificationCommand;
import com.takodu.api.conversations.domain.model.commands.MoveAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.ScheduleAppointmentNotificationsCommand;

/**
 * W11-W15: appointment notifications. Booking's events call it inside booking's transaction, so a notification
 * exists exactly when its appointment change does; those methods are idempotent. The dispatcher job sends them.
 */
public interface ScheduledMessageCommandService {

    void handle(ScheduleAppointmentNotificationsCommand command);

    void handle(MoveAppointmentNotificationsCommand command);

    void handle(CancelAppointmentNotificationsCommand command);

    /**
     * Takes the next due notification and queues it as a message, holds it (W15), or skips it (decision 65).
     *
     * @return false when nothing was due
     */
    boolean handle(DispatchNextDueNotificationCommand command);
}
