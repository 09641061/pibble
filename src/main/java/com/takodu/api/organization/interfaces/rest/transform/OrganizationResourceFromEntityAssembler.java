package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.application.queryservices.OrganizationAccount;
import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.interfaces.rest.resources.JoinedOrganizationResource;
import com.takodu.api.organization.interfaces.rest.resources.OrganizationAccountResource;
import com.takodu.api.organization.interfaces.rest.resources.OrganizationResource;

import java.util.Locale;

public final class OrganizationResourceFromEntityAssembler {
    private OrganizationResourceFromEntityAssembler() { }

    public static OrganizationResource toResourceFromEntity(Organization organization) {
        return new OrganizationResource(organization.getId().value(), organization.getName(),
                organization.getPlan().name().toLowerCase(Locale.ROOT), organization.getPlanActiveUntil());
    }

    public static JoinedOrganizationResource toResourceFromEntity(JoinedOrganization joined) {
        var organization = joined.organization();
        return new JoinedOrganizationResource(organization.getId().value(), organization.getName(),
                organization.getPlanActiveUntil(), joined.membership().getId().value(),
                joined.membership().getRole().name().toLowerCase(Locale.ROOT));
    }

    public static OrganizationAccountResource toResourceFromEntity(OrganizationAccount account) {
        var organization = account.organization();
        return new OrganizationAccountResource(organization.getId().value(), organization.getName(),
                organization.getPlan().name().toLowerCase(Locale.ROOT), organization.getPlanActiveUntil(),
                account.planActive(), account.ownerEmail() == null ? null : account.ownerEmail().value(),
                account.activeEstablishments());
    }
}
