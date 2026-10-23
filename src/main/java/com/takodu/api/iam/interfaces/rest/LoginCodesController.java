package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.commandservices.LoginCodeCommandService;
import com.takodu.api.iam.interfaces.rest.resources.RequestLoginCodeResource;
import com.takodu.api.iam.interfaces.rest.transform.RequestLoginCodeCommandFromResourceAssembler;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/api/v1/auth/login-codes")
@Tag(name = "Sign-in", description = "Passwordless sign-in with emailed codes")
public class LoginCodesController {
    private final LoginCodeCommandService loginCodeCommandService;

    public LoginCodesController(LoginCodeCommandService loginCodeCommandService) {
        this.loginCodeCommandService = loginCodeCommandService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @WithoutIdempotencyKey("a repeat emails another code, and I3 caps codes at five per hour")
    @Operation(summary = "Request sign-in code",
            description = "Emails a 6-digit code valid for 10 minutes. The answer is the same whether or not the email has an account.")
    @APIResponse(responseCode = "202", description = "Code emailed")
    @APIResponse(responseCode = "400", description = "Invalid email")
    @APIResponse(responseCode = "429", description = "Five codes were already requested for this email in the last hour")
    public void request(@Valid RequestLoginCodeResource resource) {
        loginCodeCommandService.handle(RequestLoginCodeCommandFromResourceAssembler.toCommandFromResource(resource));
    }
}
