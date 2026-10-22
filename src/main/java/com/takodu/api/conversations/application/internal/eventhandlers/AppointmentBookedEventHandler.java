package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.domain.model.commands.ScheduleAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AppointmentBookedEventHandler {
    private final ScheduledMessageCommandService scheduledMessageCommandService;

    public AppointmentBookedEventHandler(ScheduledMessageCommandService scheduledMessageCommandService) {
        this.scheduledMessageCommandService = scheduledMessageCommandService;
    }

    /** W11, W12: schedules the reminder and, for staff bookings, the confirmation, in booking's transaction. */
    void on(@Observes AppointmentBookedIntegrationEvent event) {
        scheduledMessageCommandService.handle(new ScheduleAppointmentNotificationsCommand(
                new OrganizationId(event.organizationId()), new EstablishmentId(event.establishmentId()),
                new AppointmentId(event.appointmentId()), new CustomerId(event.customerId()),
                new ServiceId(event.serviceId()), event.startsAt(), "staff".equals(event.source())));
    }
}
