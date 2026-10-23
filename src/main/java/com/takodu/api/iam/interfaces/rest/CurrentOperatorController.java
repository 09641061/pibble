package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.queryservices.UserQueryService;
import com.takodu.api.iam.domain.model.queries.GetUserByIdQuery;
import com.takodu.api.iam.interfaces.rest.resources.UserResource;
import com.takodu.api.shared.application.tenancy.OperatorContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** The Takodu team member calling the internal API. */
@Path("/internal/v1/operator")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal", description = "Takodu team only: plans and usage across organizations")
public class CurrentOperatorController {
    private final UserQueryService userQueryService;
    private final OperatorContext operatorContext;

    public CurrentOperatorController(UserQueryService userQueryService, OperatorContext operatorContext) {
        this.userQueryService = userQueryService;
        this.operatorContext = operatorContext;
    }

    @GET
    @Operation(summary = "Get the signed-in operator", description = "Lets an internal tool check it is signed in as a platform operator.")
    @APIResponse(responseCode = "200", description = "The caller is a platform operator",
            content = @Content(schema = @Schema(implementation = UserResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a platform operator, or is disabled")
    public UserResource getOperator() {
        var user = userQueryService.handle(new GetUserByIdQuery(operatorContext.userId())).orElseThrow();
        return new UserResource(user.getId().value(), user.getEmail().value(), user.getName());
    }
}
