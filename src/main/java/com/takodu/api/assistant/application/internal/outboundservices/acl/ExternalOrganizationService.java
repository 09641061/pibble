package com.takodu.api.assistant.application.internal.outboundservices.acl;

import com.takodu.api.assistant.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.assistant.domain.model.valueobjects.OpeningRange;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.ZoneId;

/** The only class in assistant that talks to organization. */
@ApplicationScoped
public class ExternalOrganizationService {
    private final OrganizationContextFacade organizationContextFacade;

    public ExternalOrganizationService(OrganizationContextFacade organizationContextFacade) {
        this.organizationContextFacade = organizationContextFacade;
    }

    /** @throws EstablishmentNotFoundException when the establishment is not in the organization */
    public EstablishmentProfile fetchEstablishment(OrganizationId organizationId, EstablishmentId establishmentId) {
        var view = organizationContextFacade.fetchCalendar(organizationId.value(), establishmentId.value())
                .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId));
        return new EstablishmentProfile(establishmentId, view.name(), view.address(), ZoneId.of(view.timeZone()),
                view.openingHours().stream()
                        .map(range -> new OpeningRange(DayOfWeek.of(range.isoWeekday()), range.opensAt(),
                                range.closesAt()))
                        .toList(),
                view.archived(), view.planActive());
    }
}
