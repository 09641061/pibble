package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.internal.outboundservices.acl.ExternalIamService;
import com.takodu.api.organization.application.queryservices.PlanChangeEntry;
import com.takodu.api.organization.application.queryservices.PlanChangeQueryService;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.queries.GetPlanChangesQuery;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.organization.domain.repositories.PlanChangeRepository;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlanChangeQueryServiceImpl implements PlanChangeQueryService {
    private final OrganizationRepository organizationRepository;
    private final PlanChangeRepository planChangeRepository;
    private final ExternalIamService externalIamService;

    public PlanChangeQueryServiceImpl(OrganizationRepository organizationRepository,
                                      PlanChangeRepository planChangeRepository, ExternalIamService externalIamService) {
        this.organizationRepository = organizationRepository;
        this.planChangeRepository = planChangeRepository;
        this.externalIamService = externalIamService;
    }

    /** A few operators make every change, so each email is looked up once. */
    @Override
    public List<PlanChangeEntry> handle(GetPlanChangesQuery query) {
        if (organizationRepository.findById(query.organizationId()).isEmpty())
            throw new OrganizationNotFoundException(query.organizationId());
        var emails = new HashMap<UserId, Optional<Email>>();
        return planChangeRepository.findByOrganization(query.organizationId()).stream()
                .map(change -> new PlanChangeEntry(change, emails.computeIfAbsent(change.getChangedBy(),
                        externalIamService::emailOf).orElse(null)))
                .toList();
    }
}
