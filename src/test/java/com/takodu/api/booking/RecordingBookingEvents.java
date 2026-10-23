package com.takodu.api.booking;

import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentCancelledIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentRescheduledIntegrationEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records booking's integration events in every test, so booking checks what it publishes apart from its listeners. */
@Singleton
public class RecordingBookingEvents {
    private final List<Object> events = new CopyOnWriteArrayList<>();

    void on(@Observes AppointmentBookedIntegrationEvent event) {
        events.add(event);
    }

    void on(@Observes AppointmentRescheduledIntegrationEvent event) {
        events.add(event);
    }

    void on(@Observes AppointmentCancelledIntegrationEvent event) {
        events.add(event);
    }

    /** @return the events published about the appointment, oldest first */
    public List<Object> about(UUID appointmentId) {
        return events.stream().filter(event -> appointmentIdOf(event).equals(appointmentId)).toList();
    }

    private static UUID appointmentIdOf(Object event) {
        return switch (event) {
            case AppointmentBookedIntegrationEvent booked -> booked.appointmentId();
            case AppointmentRescheduledIntegrationEvent rescheduled -> rescheduled.appointmentId();
            case AppointmentCancelledIntegrationEvent cancelled -> cancelled.appointmentId();
            default -> throw new IllegalStateException("Unknown event " + event);
        };
    }
}
