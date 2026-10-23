package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.application.queryservices.PlanChangeEntry;
import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.organization.interfaces.rest.resources.PlanChangeResource;
import com.takodu.api.shared.domain.model.valueobjects.Email;

import java.util.Locale;

public final class PlanChangeResourceFromEntityAssembler {
    private PlanChangeResourceFromEntityAssembler() { }

    public static PlanChangeResource toResourceFromEntity(PlanChangeEntry entry) {
        return toResourceFromEntity(entry.change(), entry.operatorEmail());
    }

    /** @param operatorEmail the acting operator's email, or null when unknown */
    public static PlanChangeResource toResourceFromEntity(PlanChange change, Email operatorEmail) {
        return new PlanChangeResource(change.getId().value(), change.getOrganizationId().value(),
                name(change.getFromPlan()), name(change.getToPlan()), change.getFromActiveUntil(),
                change.getToActiveUntil(), change.getNote(), change.getChangedBy().value(),
                operatorEmail == null ? null : operatorEmail.value(), change.getChangedAt());
    }

    static String name(Plan plan) {
        return plan.name().toLowerCase(Locale.ROOT);
    }
}
