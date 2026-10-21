package com.takodu.api.organization.interfaces.rest.filters;

import com.takodu.api.organization.application.queryservices.MemberQueryService;
import com.takodu.api.organization.domain.model.queries.GetActiveMembershipQuery;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.UUID;

/**
 * Binds {@link TenantContext} for every {@code /api/v1} request from the access token's subject and the
 * {@code X-Organization-Id} header, using the caller's active membership (takodu-tenancy).
 *
 * <p>Skipped for sign-in ({@code /api/v1/auth/**}), for listing and creating the caller's organizations
 * ({@code /api/v1/organizations}) and for accepting an invitation, which all come before the caller is a member.
 * Webhooks and {@code /q/**} are outside {@code /api/v1}.</p>
 */
public class TenantFilter {
    public static final String HEADER = "X-Organization-Id";

    private final MemberQueryService memberQueryService;
    private final TenantContext tenantContext;
    private final JsonWebToken accessToken;

    public TenantFilter(MemberQueryService memberQueryService, TenantContext tenantContext, JsonWebToken accessToken) {
        this.memberQueryService = memberQueryService;
        this.tenantContext = tenantContext;
        this.accessToken = accessToken;
    }

    /**
     * @throws AuthenticationFailedException without an access token (401)
     * @throws IllegalArgumentException when the header is missing or not a UUID (400)
     * @throws AccessDeniedException when the caller has no active membership in that organization (403)
     */
    @ServerRequestFilter
    public void bindTenant(ContainerRequestContext request) {
        if (!needsTenant(request.getUriInfo().getPath())) return;
        if (accessToken.getSubject() == null)
            throw new AuthenticationFailedException("UNAUTHENTICATED", "Sign in to use this endpoint");
        var organizationId = new OrganizationId(parse(request.getHeaderString(HEADER)));
        var userId = new UserId(UUID.fromString(accessToken.getSubject()));
        var member = memberQueryService.handle(new GetActiveMembershipQuery(organizationId, userId))
                .orElseThrow(() -> new AccessDeniedException("access this organization"));
        tenantContext.bind(userId, organizationId, member.getId(), member.getRole(), member.getEstablishmentIds());
    }

    static boolean needsTenant(String path) {
        return path.startsWith("/api/v1/")
                && !path.startsWith("/api/v1/auth/")
                && !path.equals("/api/v1/organizations")
                && !path.startsWith("/api/v1/invitation-acceptances");
    }

    private static UUID parse(String header) {
        if (header == null || header.isBlank()) throw new IllegalArgumentException(HEADER + " header is required");
        try {
            return UUID.fromString(header.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(HEADER + " header must be a UUID");
        }
    }
}
