package com.takodu.api.booking.application.internal.queryservices;

import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.booking.application.queryservices.TimeBlockQueryService;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.queries.GetTimeBlocksByEstablishmentQuery;
import com.takodu.api.booking.domain.repositories.TimeBlockRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TimeBlockQueryServiceImpl implements TimeBlockQueryService {
    private final TimeBlockRepository timeBlockRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public TimeBlockQueryServiceImpl(TimeBlockRepository timeBlockRepository,
                                     ExternalOrganizationService externalOrganizationService, AccessPolicy accessPolicy) {
        this.timeBlockRepository = timeBlockRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<TimeBlock> handle(GetTimeBlocksByEstablishmentQuery query) {
        externalOrganizationService.fetchCalendar(query.organizationId(), query.establishmentId());
        accessPolicy.requireEstablishment(query.establishmentId());
        return timeBlockRepository.findOverlapping(query.organizationId(), query.establishmentId(), query.window());
    }
}
