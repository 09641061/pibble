package com.takodu.api.conversations.application.internal.outboundservices.acl;

import com.takodu.api.conversations.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.conversations.domain.model.valueobjects.EstablishmentState;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

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
        return new EstablishmentState(establishmentId, view.archived(), view.planActive());
    }
}
