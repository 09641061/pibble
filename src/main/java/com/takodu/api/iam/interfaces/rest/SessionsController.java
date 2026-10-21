package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.commandservices.SessionCommandService;
import com.takodu.api.iam.interfaces.rest.resources.SessionTokensResource;
import com.takodu.api.iam.interfaces.rest.resources.SignInResource;
import com.takodu.api.iam.interfaces.rest.transform.SessionTokensResourceFromEntityAssembler;
import com.takodu.api.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
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
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/api/v1/auth/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sign-in", description = "Passwordless sign-in with emailed codes")
public class SessionsController {
    private final SessionCommandService sessionCommandService;

    public SessionsController(SessionCommandService sessionCommandService) {
        this.sessionCommandService = sessionCommandService;
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
}
