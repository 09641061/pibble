package com.takodu.api.organization.interfaces.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What other contexts may ask organization. No method checks the caller's role or establishments, because jobs and
 * the assistant ask too; the asking context checks access with {@code AccessPolicy}.
 */
public interface OrganizationContextFacade {

    /** @return the establishment's time zone, opening hours and plan state, empty when not in the organization */
    Optional<EstablishmentCalendarView> fetchCalendar(UUID organizationId, UUID establishmentId);

    /** @return the service, active or not, with who can perform it now; empty when not in the organization */
    Optional<ServiceView> fetchService(UUID organizationId, UUID serviceId);

    /** @return the establishment's active services, by name; empty when it is not in the organization */
    List<ServiceView> fetchActiveServices(UUID organizationId, UUID establishmentId);

    /**
     * @param establishmentId the establishment {@link MemberView#worksThere()} refers to
     * @return the member, empty when removed, still invited or not in the organization
     */
    Optional<MemberView> fetchMember(UUID organizationId, UUID memberId, UUID establishmentId);
}
