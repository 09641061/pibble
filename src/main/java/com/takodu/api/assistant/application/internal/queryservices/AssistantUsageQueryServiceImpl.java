package com.takodu.api.assistant.application.internal.queryservices;

import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.application.queryservices.AssistantUsageQueryService;
import com.takodu.api.assistant.domain.model.queries.GetEstablishmentUsageQuery;
import com.takodu.api.assistant.domain.model.queries.GetOrganizationUsageQuery;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentUsage;
import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.assistant.domain.repositories.AssistantTurnRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class AssistantUsageQueryServiceImpl implements AssistantUsageQueryService {
    private final AssistantTurnRepository assistantTurnRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public AssistantUsageQueryServiceImpl(AssistantTurnRepository assistantTurnRepository,
                                          ExternalOrganizationService externalOrganizationService,
                                          AccessPolicy accessPolicy) {
        this.assistantTurnRepository = assistantTurnRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public EstablishmentUsage handle(GetEstablishmentUsageQuery query) {
        accessPolicy.require(Action.MANAGE_ASSISTANT);
        return usageOf(query.organizationId(), query.establishmentId(), query.period());
    }

    /** Establishments may sit in different time zones, so each one's days are read in its own zone (O7). */
    @Override
    public List<EstablishmentUsage> handle(GetOrganizationUsageQuery query) {
        externalOrganizationService.fetchOrganizationName(query.organizationId());
        var period = query.period();
        return assistantTurnRepository.establishmentsWithUsage(query.organizationId(), period.earliestStart(),
                        period.latestEnd()).stream()
                .map(establishmentId -> usageOf(query.organizationId(), establishmentId, period))
                .filter(usage -> !usage.days().isEmpty())
                .sorted(Comparator.comparing(EstablishmentUsage::establishmentName))
                .toList();
    }

    private EstablishmentUsage usageOf(OrganizationId organizationId, EstablishmentId establishmentId,
                                       UsagePeriod period) {
        var establishment = externalOrganizationService.fetchEstablishment(organizationId, establishmentId);
        var zone = establishment.zone();
        return new EstablishmentUsage(establishmentId, establishment.name(), zone, period,
                assistantTurnRepository.dailyUsage(organizationId, establishmentId, zone, period.start(zone),
                        period.end(zone)));
    }
}
