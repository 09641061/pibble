package com.takodu.api.organization.application.internal.outboundservices.acl;

import com.takodu.api.organization.interfaces.acl.UpcomingAppointmentsProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Replaces booking's answer in every {@code @QuarkusTest}: a test marks the members that have appointments ahead. */
@Alternative
@Priority(1)
@Singleton
public class FakeUpcomingAppointments implements UpcomingAppointmentsProvider {
    private final Set<UUID> booked = ConcurrentHashMap.newKeySet();

    public void book(UUID memberId) {
        booked.add(memberId);
    }

    @Override
    public boolean hasUpcomingAppointments(UUID organizationId, UUID memberId, Instant now) {
        return booked.contains(memberId);
    }
}
