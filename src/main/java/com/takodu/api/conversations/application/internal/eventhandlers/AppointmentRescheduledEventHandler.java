package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.booking.interfaces.events.AppointmentRescheduledIntegrationEvent;
import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.domain.model.commands.MoveAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AppointmentRescheduledEventHandler {
    private final ScheduledMessageCommandService scheduledMessageCommandService;

    public AppointmentRescheduledEventHandler(ScheduledMessageCommandService scheduledMessageCommandService) {
        this.scheduledMessageCommandService = scheduledMessageCommandService;
    }

    /** W13: moves the reminder with the appointment, in booking's transaction. */
    void on(@Observes AppointmentRescheduledIntegrationEvent event) {
        scheduledMessageCommandService.handle(new MoveAppointmentNotificationsCommand(
                new OrganizationId(event.organizationId()), new EstablishmentId(event.establishmentId()),
                new AppointmentId(event.appointmentId()), new CustomerId(event.customerId()),
                new ServiceId(event.serviceId()), event.startsAt()));
    }
}
