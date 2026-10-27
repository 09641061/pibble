package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentCancelledIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentRescheduledIntegrationEvent;
import com.takodu.api.conversations.domain.model.events.AppointmentChangedEvent;
import com.takodu.api.conversations.domain.model.events.AssistantActivityEvent;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.InboxEvent;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import java.util.UUID;

/**
 * Forwards committed inbox and appointment changes, and the assistant's activity as it happens, to every open
 * stream of this instance. Only an optimization: an app that missed a change, or reconnects, reloads the inbox.
 */
@ApplicationScoped
public class ConversationChangedEventHandler {
    /** Serialized, because commits on several threads publish at once. */
    private final SerializedProcessor<InboxEvent, InboxEvent> changes =
            BroadcastProcessor.<InboxEvent>create().serialized();

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) ConversationChangedEvent event) {
        changes.onNext(event);
    }

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) AppointmentBookedIntegrationEvent event) {
        changes.onNext(appointment(event.organizationId(), event.establishmentId(), event.appointmentId(),
                AppointmentChangedEvent.Change.BOOKED));
    }

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) AppointmentRescheduledIntegrationEvent event) {
        changes.onNext(appointment(event.organizationId(), event.establishmentId(), event.appointmentId(),
                AppointmentChangedEvent.Change.RESCHEDULED));
    }

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) AppointmentCancelledIntegrationEvent event) {
        changes.onNext(appointment(event.organizationId(), event.establishmentId(), event.appointmentId(),
                AppointmentChangedEvent.Change.CANCELLED));
    }

    /** Turns run outside transactions, so their activity goes out at once. */
    void on(@Observes AssistantActivityEvent event) {
        changes.onNext(event);
    }

    private static AppointmentChangedEvent appointment(UUID organizationId, UUID establishmentId, UUID appointmentId,
                                                       AppointmentChangedEvent.Change change) {
        return new AppointmentChangedEvent(new OrganizationId(organizationId), new EstablishmentId(establishmentId),
                new AppointmentId(appointmentId), change);
    }

    /** @return the inbox events from now on, until the subscriber cancels */
    public Multi<InboxEvent> changes() {
        return Multi.createFrom().publisher(changes);
    }
}
