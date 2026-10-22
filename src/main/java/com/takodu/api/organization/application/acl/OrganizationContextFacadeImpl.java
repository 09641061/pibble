package com.takodu.api.organization.application.acl;

import com.takodu.api.organization.application.queryservices.EstablishmentQueryService;
import com.takodu.api.organization.application.queryservices.MemberQueryService;
import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.application.queryservices.ServiceQueryService;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.queries.GetBookableServiceQuery;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentCalendarQuery;
import com.takodu.api.organization.domain.model.queries.GetMemberByIdQuery;
import com.takodu.api.organization.domain.model.queries.GetOrganizationByIdQuery;
import com.takodu.api.organization.interfaces.acl.EstablishmentCalendarView;
import com.takodu.api.organization.interfaces.acl.MemberView;
import com.takodu.api.organization.interfaces.acl.OpeningRangeView;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.organization.interfaces.acl.ServiceView;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrganizationContextFacadeImpl implements OrganizationContextFacade {
    private final OrganizationQueryService organizationQueryService;
    private final EstablishmentQueryService establishmentQueryService;
    private final ServiceQueryService serviceQueryService;
    private final MemberQueryService memberQueryService;
    private final Clock clock;

    public OrganizationContextFacadeImpl(OrganizationQueryService organizationQueryService,
                                         EstablishmentQueryService establishmentQueryService,
                                         ServiceQueryService serviceQueryService, MemberQueryService memberQueryService,
                                         Clock clock) {
        this.organizationQueryService = organizationQueryService;
        this.establishmentQueryService = establishmentQueryService;
        this.serviceQueryService = serviceQueryService;
        this.memberQueryService = memberQueryService;
        this.clock = clock;
    }

    @Override
    public Optional<EstablishmentCalendarView> fetchCalendar(UUID organizationId, UUID establishmentId) {
        var organization = new OrganizationId(organizationId);
        return establishmentQueryService.handle(new GetEstablishmentCalendarQuery(organization,
                new EstablishmentId(establishmentId))).map(establishment -> calendar(establishment,
                organizationQueryService.handle(new GetOrganizationByIdQuery(organization))
                        .map(found -> found.isPlanActive(clock.instant())).orElse(false)));
    }

    @Override
    public Optional<ServiceView> fetchService(UUID organizationId, UUID serviceId) {
        return serviceQueryService.handle(new GetBookableServiceQuery(new OrganizationId(organizationId),
                new ServiceId(serviceId))).map(bookable -> {
            var service = bookable.service();
            return new ServiceView(service.getId().value(), service.getEstablishmentId().value(), service.getName(),
                    (int) service.getDuration().toMinutes(), service.getPrice().cents(), service.getPrice().currency(),
                    service.isActive(), bookable.performers().stream().map(member -> member.getId().value()).toList());
        });
    }

    @Override
    public Optional<MemberView> fetchMember(UUID organizationId, UUID memberId, UUID establishmentId) {
        return memberQueryService.handle(new GetMemberByIdQuery(new OrganizationId(organizationId), new MemberId(memberId)))
                .filter(Member::isActive)
                .map(member -> new MemberView(member.getId().value(), member.getDisplayName(),
                        member.worksAt(new EstablishmentId(establishmentId))));
    }

    private static EstablishmentCalendarView calendar(Establishment establishment, boolean planActive) {
        var openingHours = establishment.getSchedule().ranges().stream()
                .map(range -> new OpeningRangeView(range.day().getValue(), range.opensAt(), range.closesAt()))
                .toList();
        return new EstablishmentCalendarView(establishment.getId().value(), establishment.getName(),
                establishment.getTimeZone().value().getId(),
                openingHours, !establishment.isActive(), planActive);
    }
}
