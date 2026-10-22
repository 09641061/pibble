package com.takodu.api.shared.application.tenancy;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.RequestScoped;

import java.util.Objects;
import java.util.Set;

/**
 * Who is calling and in which organization, for one request. {@code organization}'s {@code TenantFilter} fills it
 * from the access token and the {@code X-Organization-Id} header; every other class only reads it.
 *
 * <p>Webhooks, jobs and event handlers have no request, so they never inject it (takodu-tenancy). The one exception
 * is an assistant turn, which binds the assistant to its establishment for the length of the turn, so the services it
 * calls check it with the same {@link AccessPolicy} as staff.</p>
 */
@RequestScoped
public class TenantContext {
    private UserId userId;
    private OrganizationId organizationId;
    private MemberId memberId;
    private Role role;
    private Set<EstablishmentId> establishmentIds = Set.of();
    private boolean assistant;

    /**
     * Called once per request by {@code TenantFilter} with the caller's active membership.
     *
     * @param establishmentIds the establishments a {@code staff} member is assigned to; ignored for other roles
     */
    public void bind(UserId userId, OrganizationId organizationId, MemberId memberId, Role role,
                     Set<EstablishmentId> establishmentIds) {
        if (this.organizationId != null) throw new IllegalStateException("TenantContext is already bound");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.memberId = Objects.requireNonNull(memberId, "memberId is required");
        this.role = Objects.requireNonNull(role, "role is required");
        this.establishmentIds = role.seesAllEstablishments() ? Set.of() : Set.copyOf(establishmentIds);
    }

    /**
     * A4: binds the assistant for one turn. It is no member and has no role; it acts in one establishment only, and
     * {@link Action#allowsAssistant()} lists what it may do there.
     */
    public void bindAssistant(OrganizationId organizationId, EstablishmentId establishmentId) {
        if (this.organizationId != null) throw new IllegalStateException("TenantContext is already bound");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentIds = Set.of(Objects.requireNonNull(establishmentId, "establishmentId is required"));
        this.assistant = true;
    }

    /** @return whether the assistant, not a person, is acting */
    public boolean isAssistant() {
        requireBound();
        return assistant;
    }

    public UserId userId() {
        requirePerson();
        requireBound();
        return userId;
    }

    public OrganizationId organizationId() {
        requireBound();
        return organizationId;
    }

    public MemberId memberId() {
        requirePerson();
        return memberId;
    }

    public Role role() {
        requirePerson();
        return role;
    }

    /**
     * @return the establishments a {@code staff} caller is assigned to, or the assistant's one; empty for owners and
     *         managers, who see all
     */
    public Set<EstablishmentId> establishmentIds() {
        requireBound();
        return establishmentIds;
    }

    /**
     * O13: owners and managers see every establishment, staff only their assigned ones, and the assistant only its
     * own.
     *
     * @param establishmentId an establishment of the caller's organization
     */
    public boolean canSee(EstablishmentId establishmentId) {
        requireBound();
        return assistant ? establishmentIds.contains(establishmentId)
                : role().seesAllEstablishments() || establishmentIds.contains(establishmentId);
    }

    /** The assistant has no user, member or role, so code that needs a person fails closed for it. */
    private void requirePerson() {
        requireBound();
        if (assistant) throw new IllegalStateException("The assistant is acting, not a member");
    }

    private void requireBound() {
        if (organizationId == null)
            throw new IllegalStateException("No tenant for this request: the endpoint needs the X-Organization-Id header");
    }
}
