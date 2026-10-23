package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.domain.model.queries.GetOrganizationAccountsQuery;
import com.takodu.api.organization.interfaces.rest.resources.OrganizationAccountResource;
import com.takodu.api.organization.interfaces.rest.transform.OrganizationResourceFromEntityAssembler;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Every organization, for the Takodu team following payments (O5). {@code OperatorFilter} admits only operators. */
@Path("/internal/v1/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal", description = "Takodu team only: plans and usage across organizations")
public class OrganizationAccountsController {
    private final OrganizationQueryService organizationQueryService;

    public OrganizationAccountsController(OrganizationQueryService organizationQueryService) {
        this.organizationQueryService = organizationQueryService;
    }

    @GET
    @Operation(summary = "List organizations", description = "Every organization with its plan, owner email and active establishments.")
    @APIResponse(responseCode = "200", description = "Organizations, the plan ending soonest first",
            content = @Content(schema = @Schema(implementation = OrganizationAccountResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a platform operator")
    public List<OrganizationAccountResource> getOrganizations() {
        return organizationQueryService.handle(new GetOrganizationAccountsQuery()).stream()
                .map(OrganizationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
