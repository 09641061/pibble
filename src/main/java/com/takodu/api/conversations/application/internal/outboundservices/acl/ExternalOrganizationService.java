package com.takodu.api.conversations.application.internal.outboundservices.acl;

import com.takodu.api.conversations.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.conversations.domain.exceptions.MemberNotAtEstablishmentException;
import com.takodu.api.conversations.domain.exceptions.MemberNotFoundException;
import com.takodu.api.conversations.domain.model.valueobjects.EstablishmentState;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.organization.interfaces.acl.ServiceView;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.ZoneId;
import java.util.Optional;

/** The only class in conversations that talks to organization. */
@ApplicationScoped
public class ExternalOrganizationService {
    private final OrganizationContextFacade organizationContextFacade;

    public ExternalOrganizationService(OrganizationContextFacade organizationContextFacade) {
        this.organizationContextFacade = organizationContextFacade;
    }

    /** @throws EstablishmentNotFoundException when the establishment is not in the organization */
    public EstablishmentState fetchEstablishment(OrganizationId organizationId, EstablishmentId establishmentId) {
        var view = organizationContextFacade.fetchCalendar(organizationId.value(), establishmentId.value())
                .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId));
        return new EstablishmentState(establishmentId, view.name(), ZoneId.of(view.timeZone()), view.archived(),
                view.planActive());
    }

    /** @return the service's name, active or not; empty when it is not in the organization */
    public Optional<String> fetchServiceName(OrganizationId organizationId, ServiceId serviceId) {
        return organizationContextFacade.fetchService(organizationId.value(), serviceId.value()).map(ServiceView::name);
    }

    /**
     * @throws MemberNotFoundException when the member is removed, still invited or not in the organization
     * @throws MemberNotAtEstablishmentException when the member cannot see the establishment's inbox (O13)
     */
    public void requireMemberWorksAt(OrganizationId organizationId, MemberId memberId, EstablishmentId establishmentId) {
        var member = organizationContextFacade.fetchMember(organizationId.value(), memberId.value(), establishmentId.value())
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.worksThere()) throw new MemberNotAtEstablishmentException(memberId);
    }
}
