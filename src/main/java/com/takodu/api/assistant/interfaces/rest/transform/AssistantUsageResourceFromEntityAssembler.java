package com.takodu.api.assistant.interfaces.rest.transform;

import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentUsage;
import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.assistant.domain.model.valueobjects.UsageTotals;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantUsageResource;
import com.takodu.api.assistant.interfaces.rest.resources.DailyUsageResource;
import com.takodu.api.assistant.interfaces.rest.resources.OrganizationAssistantUsageResource;
import com.takodu.api.assistant.interfaces.rest.resources.UsageTotalsResource;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.List;

public final class AssistantUsageResourceFromEntityAssembler {
    private AssistantUsageResourceFromEntityAssembler() { }

    public static AssistantUsageResource toResourceFromEntity(EstablishmentUsage usage) {
        return new AssistantUsageResource(usage.establishmentId().value(), usage.establishmentName(),
                usage.zone().getId(), usage.period().from(), usage.period().to(), totals(usage.totals()),
                usage.days().stream().map(day -> new DailyUsageResource(day.date(), totals(day.totals()))).toList());
    }

    public static OrganizationAssistantUsageResource toResourceFromEntity(OrganizationId organizationId,
                                                                          UsagePeriod period,
                                                                          List<EstablishmentUsage> establishments) {
        var sum = establishments.stream().map(EstablishmentUsage::totals).reduce(UsageTotals.ZERO, UsageTotals::plus);
        return new OrganizationAssistantUsageResource(organizationId.value(), period.from(), period.to(), totals(sum),
                establishments.stream().map(AssistantUsageResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    private static UsageTotalsResource totals(UsageTotals totals) {
        return new UsageTotalsResource(totals.turns(), totals.inputTokens(), totals.cachedTokens(),
                totals.outputTokens(), totals.toolCalls());
    }
}
