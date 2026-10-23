package com.takodu.api.assistant.application.internal.outboundservices.acl;

import com.takodu.api.assistant.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.assistant.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.assistant.domain.model.valueobjects.OfferedService;
import com.takodu.api.assistant.domain.model.valueobjects.OpeningRange;
import com.takodu.api.organization.interfaces.acl.MemberView;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** The only class in assistant that talks to organization. */
@ApplicationScoped
public class ExternalOrganizationService {
    private final OrganizationContextFacade organizationContextFacade;

    public ExternalOrganizationService(OrganizationContextFacade organizationContextFacade) {
        this.organizationContextFacade = organizationContextFacade;
    }

    /** @return the establishment's active services, by name */
    public List<OfferedService> fetchActiveServices(OrganizationId organizationId, EstablishmentId establishmentId) {
        return organizationContextFacade.fetchActiveServices(organizationId.value(), establishmentId.value()).stream()
                .map(view -> new OfferedService(new ServiceId(view.id()), view.name(),
                        Duration.ofMinutes(view.durationMinutes()), new Money(view.priceCents(), view.currency()),
                        view.instructions(), view.performerIds().stream().map(MemberId::new).toList()))
                .toList();
    }

    /** @return the member's display name, empty when removed or not in the organization */
    public Optional<String> fetchMemberName(OrganizationId organizationId, MemberId memberId,
                                            EstablishmentId establishmentId) {
        return organizationContextFacade.fetchMember(organizationId.value(), memberId.value(), establishmentId.value())
                .map(MemberView::displayName);
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

    /** @throws OrganizationNotFoundException when no organization has the id */
    public String fetchOrganizationName(OrganizationId organizationId) {
        return organizationContextFacade.fetchOrganizationName(organizationId.value())
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
