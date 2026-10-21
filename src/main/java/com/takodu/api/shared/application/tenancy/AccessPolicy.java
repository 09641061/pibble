package com.takodu.api.shared.application.tenancy;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.enterprise.context.RequestScoped;

import java.util.Locale;

/** Checks the caller in {@link TenantContext} against the fixed permission table in {@link Action}. */
@RequestScoped
public class AccessPolicy {
    private final TenantContext tenantContext;

    public AccessPolicy(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * @throws AccessDeniedException when the caller's role may not perform the action
     */
    public void require(Action action) {
        if (!action.allows(tenantContext.role()))
            throw new AccessDeniedException(action.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    /**
     * O13: staff act only in their assigned establishments; owners and managers in all.
     *
     * @throws AccessDeniedException when the caller is staff not assigned to the establishment
     */
    public void requireEstablishment(EstablishmentId establishmentId) {
        if (!tenantContext.canSee(establishmentId)) throw new AccessDeniedException("access this establishment");
    }
}
