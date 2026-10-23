package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.PlanCommandService;
import com.takodu.api.organization.application.queryservices.PlanChangeQueryService;
import com.takodu.api.organization.domain.model.queries.GetPlanChangesQuery;
import com.takodu.api.organization.interfaces.rest.resources.ChangePlanResource;
import com.takodu.api.organization.interfaces.rest.resources.PlanChangeResource;
import com.takodu.api.organization.interfaces.rest.transform.ChangePlanCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.PlanChangeResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.OperatorContext;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

/** The Takodu team records payments as plan changes (O5). {@code OperatorFilter} admits only operators. */
@Path("/internal/v1/organizations/{organizationId}/plan-changes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal", description = "Takodu team only: plans and usage across organizations")
public class PlanChangesController {
    private final PlanCommandService planCommandService;
    private final PlanChangeQueryService planChangeQueryService;
    private final OperatorContext operatorContext;

    public PlanChangesController(PlanCommandService planCommandService, PlanChangeQueryService planChangeQueryService,
                                 OperatorContext operatorContext) {
        this.planCommandService = planCommandService;
        this.planChangeQueryService = planChangeQueryService;
        this.operatorContext = operatorContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Change the plan", description = "Sets the plan and the end of the paid period after a payment, and appends the change to the history.")
    @APIResponse(responseCode = "201", description = "Plan changed, or the change an earlier request with the same key made",
            content = @Content(schema = @Schema(implementation = PlanChangeResource.class)))
    @APIResponse(responseCode = "400", description = "Both or neither of the fields, a blank note, or missing Idempotency-Key")
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a platform operator")
    @APIResponse(responseCode = "404", description = "No organization has this id")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    public PlanChangeResource changePlan(@PathParam("organizationId") UUID organizationId,
                                         @HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                         @Valid @NotNull ChangePlanResource resource) {
        var command = ChangePlanCommandFromResourceAssembler.toCommandFromResource(new OrganizationId(organizationId),
                operatorContext.userId(), idempotencyKey, resource);
        var change = planCommandService.handle(command);
        return planChangeQueryService.handle(new GetPlanChangesQuery(change.getOrganizationId())).stream()
                .filter(entry -> entry.change().getId().equals(change.getId()))
                .findFirst().map(PlanChangeResourceFromEntityAssembler::toResourceFromEntity).orElseThrow();
    }

    @GET
    @Operation(summary = "List plan changes", description = "The organization's append-only plan history, to answer a payment dispute.")
    @APIResponse(responseCode = "200", description = "Plan changes, newest first",
            content = @Content(schema = @Schema(implementation = PlanChangeResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a platform operator")
    @APIResponse(responseCode = "404", description = "No organization has this id")
    public List<PlanChangeResource> getPlanChanges(@PathParam("organizationId") UUID organizationId) {
        return planChangeQueryService.handle(new GetPlanChangesQuery(new OrganizationId(organizationId))).stream()
                .map(PlanChangeResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
