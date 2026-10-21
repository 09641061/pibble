package com.takodu.api.organization.application.internal.outboundservices.acl;

import com.takodu.api.organization.interfaces.acl.UpcomingAppointmentsProvider;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

/** Used while no context implements {@link UpcomingAppointmentsProvider}: before booking exists, nobody has any. */
@ApplicationScoped
@DefaultBean
public class NoUpcomingAppointments implements UpcomingAppointmentsProvider {

    @Override
    public boolean hasUpcomingAppointments(UUID organizationId, UUID memberId, Instant now) {
        return false;
    }
}
