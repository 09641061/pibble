package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.model.commands.CancelAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.MoveAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.ScheduleAppointmentNotificationsCommand;

/**
 * W11-W15: appointment notifications. Booking's events call it inside booking's transaction, so a notification
 * exists exactly when its appointment change does; every method is idempotent.
 */
public interface ScheduledMessageCommandService {

    void handle(ScheduleAppointmentNotificationsCommand command);

    void handle(MoveAppointmentNotificationsCommand command);

    void handle(CancelAppointmentNotificationsCommand command);
}
