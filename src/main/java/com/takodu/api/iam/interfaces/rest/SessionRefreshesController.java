package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.commandservices.SessionCommandService;
import com.takodu.api.iam.interfaces.rest.resources.RefreshSessionResource;
import com.takodu.api.iam.interfaces.rest.resources.SessionTokensResource;
import com.takodu.api.iam.interfaces.rest.transform.RefreshSessionCommandFromResourceAssembler;
import com.takodu.api.iam.interfaces.rest.transform.SessionTokensResourceFromEntityAssembler;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/auth/session-refreshes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sign-in", description = "Passwordless sign-in with emailed codes")
public class SessionRefreshesController {
    private final SessionCommandService sessionCommandService;

    public SessionRefreshesController(SessionCommandService sessionCommandService) {
        this.sessionCommandService = sessionCommandService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @WithoutIdempotencyKey("the previous refresh token is accepted again for two minutes after a rotation")
    @Operation(summary = "Refresh session",
            description = "Rotates the refresh token and returns a new access token. Retrying with the previous token within two minutes works; later it ends the session.")
    @APIResponse(responseCode = "200", description = "New tokens",
            content = @Content(schema = @Schema(implementation = SessionTokensResource.class)))
    @APIResponse(responseCode = "400", description = "Missing refresh token")
    @APIResponse(responseCode = "401", description = "The session has ended; sign in again")
    @APIResponse(responseCode = "403", description = "The user is disabled")
    public SessionTokensResource refresh(@Valid RefreshSessionResource resource) {
        var refreshed = sessionCommandService.handle(RefreshSessionCommandFromResourceAssembler.toCommandFromResource(resource));
        return SessionTokensResourceFromEntityAssembler.toResourceFromEntity(refreshed);
    }
}
