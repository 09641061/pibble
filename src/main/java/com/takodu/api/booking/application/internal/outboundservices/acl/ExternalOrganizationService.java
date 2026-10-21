package com.takodu.api.booking.application.internal.outboundservices.acl;

import com.takodu.api.booking.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.booking.domain.exceptions.MemberNotAtEstablishmentException;
import com.takodu.api.booking.domain.exceptions.MemberNotFoundException;
import com.takodu.api.booking.domain.model.valueobjects.EstablishmentCalendar;
import com.takodu.api.booking.domain.model.valueobjects.OpeningWindow;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.ZoneId;

/** The only class in booking that talks to organization; it turns organization's views into booking's types. */
@ApplicationScoped
public class ExternalOrganizationService {
    private final OrganizationContextFacade organizationContextFacade;

    public ExternalOrganizationService(OrganizationContextFacade organizationContextFacade) {
        this.organizationContextFacade = organizationContextFacade;
    }

    /** @throws EstablishmentNotFoundException when the establishment is not in the organization */
    public EstablishmentCalendar fetchCalendar(OrganizationId organizationId, EstablishmentId establishmentId) {
        var view = organizationContextFacade.fetchCalendar(organizationId.value(), establishmentId.value())
                .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId));
        var windows = view.openingHours().stream()
                .map(range -> new OpeningWindow(DayOfWeek.of(range.isoWeekday()), range.opensAt(), range.closesAt()))
                .toList();
        return new EstablishmentCalendar(establishmentId, ZoneId.of(view.timeZone()), windows, view.archived(),
                view.planActive());
    }

    /**
     * @throws MemberNotFoundException when the member is removed, still invited or not in the organization
     * @throws MemberNotAtEstablishmentException when the member does not work at the establishment (O13)
     */
    public void requireMemberWorksAt(OrganizationId organizationId, MemberId memberId, EstablishmentId establishmentId) {
        var member = organizationContextFacade.fetchMember(organizationId.value(), memberId.value(), establishmentId.value())
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.worksThere()) throw new MemberNotAtEstablishmentException(memberId);
    }
}
