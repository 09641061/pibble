package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.ScheduledMessage;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduledMessageRepository {

    /**
     * @return the appointment's notifications, at most one per kind (W14), locked {@code FOR UPDATE} so a change
     *         waits for a dispatcher that is sending one of them
     */
    List<ScheduledMessage> lockByAppointment(OrganizationId organizationId, AppointmentId appointmentId);

    /**
     * Locks the pending notification due first by {@code now} with {@code FOR UPDATE SKIP LOCKED}, so two
     * dispatchers never take the same one.
     */
    @CrossTenant("The dispatcher claims due notifications of every organization; the row carries its organization")
    Optional<ScheduledMessage> claimNextDue(Instant now);

    ScheduledMessage save(ScheduledMessage message);
}
