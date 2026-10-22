package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.booking.interfaces.events.AppointmentCancelledIntegrationEvent;
import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.domain.model.commands.CancelAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AppointmentCancelledEventHandler {
    private final ScheduledMessageCommandService scheduledMessageCommandService;

    public AppointmentCancelledEventHandler(ScheduledMessageCommandService scheduledMessageCommandService) {
        this.scheduledMessageCommandService = scheduledMessageCommandService;
    }

    /** W13: stops what is pending and, when staff cancelled, tells the customer; in booking's transaction. */
    void on(@Observes AppointmentCancelledIntegrationEvent event) {
        scheduledMessageCommandService.handle(new CancelAppointmentNotificationsCommand(
                new OrganizationId(event.organizationId()), new EstablishmentId(event.establishmentId()),
                new AppointmentId(event.appointmentId()), new CustomerId(event.customerId()),
                new ServiceId(event.serviceId()), event.startsAt(), "staff".equals(event.cancelledBy()),
                event.reason()));
    }
}
