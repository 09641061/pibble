package com.takodu.api.iam.interfaces.rest.filters;

import com.takodu.api.iam.application.queryservices.UserQueryService;
import com.takodu.api.iam.domain.model.queries.GetUserByIdQuery;
import com.takodu.api.shared.application.tenancy.OperatorContext;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.UUID;

/**
 * Admits only the Takodu team to {@code /internal/**} and binds {@link OperatorContext}
 * (docs/decisions/implemented/2026-10-23-takodu-team-as-platform-operators.md). The flag is read from the
 * database on every request, so removing it takes effect at once rather than when the access token expires.
 */
public class OperatorFilter {
    private final UserQueryService userQueryService;
    private final OperatorContext operatorContext;
    private final JsonWebToken accessToken;

    public OperatorFilter(UserQueryService userQueryService, OperatorContext operatorContext, JsonWebToken accessToken) {
        this.userQueryService = userQueryService;
        this.operatorContext = operatorContext;
        this.accessToken = accessToken;
    }

    /**
     * @throws AuthenticationFailedException without an access token (401)
     * @throws AccessDeniedException when the user is disabled or not a platform operator (403)
     */
    @ServerRequestFilter
    public void bindOperator(ContainerRequestContext request) {
        if (!request.getUriInfo().getPath().startsWith("/internal/")) return;
        if (accessToken.getSubject() == null)
            throw new AuthenticationFailedException("UNAUTHENTICATED", "Sign in to use this endpoint");
        var userId = new UserId(UUID.fromString(accessToken.getSubject()));
        var user = userQueryService.handle(new GetUserByIdQuery(userId))
                .orElseThrow(() -> new AccessDeniedException("use the internal API"));
        user.requirePlatformOperator();
        operatorContext.bind(userId);
    }
}
