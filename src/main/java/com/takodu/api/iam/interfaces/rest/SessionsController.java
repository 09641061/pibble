package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.commandservices.SessionCommandService;
import com.takodu.api.iam.application.queryservices.SessionQueryService;
import com.takodu.api.iam.domain.model.commands.RevokeSessionCommand;
import com.takodu.api.iam.domain.model.queries.GetActiveSessionsByUserQuery;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.iam.interfaces.rest.resources.SessionResource;
import com.takodu.api.iam.interfaces.rest.resources.SessionTokensResource;
import com.takodu.api.iam.interfaces.rest.resources.SignInResource;
import com.takodu.api.iam.interfaces.rest.transform.SessionResourceFromEntityAssembler;
import com.takodu.api.iam.interfaces.rest.transform.SessionTokensResourceFromEntityAssembler;
import com.takodu.api.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/auth/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sign-in", description = "Passwordless sign-in with emailed codes")
public class SessionsController {
    private final SessionCommandService sessionCommandService;
    private final SessionQueryService sessionQueryService;
    private final JsonWebToken accessToken;

    public SessionsController(SessionCommandService sessionCommandService, SessionQueryService sessionQueryService,
                              JsonWebToken accessToken) {
        this.sessionCommandService = sessionCommandService;
        this.sessionQueryService = sessionQueryService;
        this.accessToken = accessToken;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @WithoutIdempotencyKey("a code works once; after a lost response the person requests a new code")
    @Operation(summary = "Sign in", description = "Exchanges an emailed code for a session on this device. The first sign-in creates the user.")
    @APIResponse(responseCode = "201", description = "Signed in",
            content = @Content(schema = @Schema(implementation = SessionTokensResource.class)))
    @APIResponse(responseCode = "400", description = "Invalid email, or the code is not 6 digits")
    @APIResponse(responseCode = "401", description = "The code is wrong, used, expired or out of attempts")
    @APIResponse(responseCode = "403", description = "The user is disabled")
    public SessionTokensResource signIn(@Valid SignInResource resource) {
        var signedIn = sessionCommandService.handle(SignInCommandFromResourceAssembler.toCommandFromResource(resource));
        return SessionTokensResourceFromEntityAssembler.toResourceFromEntity(signedIn);
    }

    @GET
    @Authenticated
    @Operation(summary = "List my sessions", description = "The devices where the caller is signed in.")
    @APIResponse(responseCode = "200", description = "Active sessions, most recently used first",
            content = @Content(schema = @Schema(implementation = SessionResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    public List<SessionResource> getMySessions() {
        var current = currentSessionId();
        return sessionQueryService.handle(new GetActiveSessionsByUserQuery(callerId())).stream()
                .map(session -> SessionResourceFromEntityAssembler.toResourceFromEntity(session, current))
                .toList();
    }

    @DELETE
    @Path("/{sessionId}")
    @Authenticated
    @ResponseStatus(204)
    @Operation(summary = "Sign a device out", description = "Ends one of the caller's sessions; its access tokens stop working within 15 minutes.")
    @APIResponse(responseCode = "204", description = "Session ended")
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "404", description = "The caller has no session with this id")
    public void revoke(@PathParam("sessionId") UUID sessionId) {
        sessionCommandService.handle(new RevokeSessionCommand(callerId(), new SessionId(sessionId)));
    }

    private UserId callerId() {
        return new UserId(UUID.fromString(accessToken.getSubject()));
    }

    private SessionId currentSessionId() {
        return new SessionId(UUID.fromString(accessToken.getClaim("sid")));
    }
}
