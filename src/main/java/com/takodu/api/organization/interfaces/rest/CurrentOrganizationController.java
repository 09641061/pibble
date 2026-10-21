package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.OrganizationCommandService;
import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.commands.RenameOrganizationCommand;
import com.takodu.api.organization.domain.model.queries.GetOrganizationByIdQuery;
import com.takodu.api.organization.interfaces.rest.resources.OrganizationResource;
import com.takodu.api.organization.interfaces.rest.resources.RenameOrganizationResource;
import com.takodu.api.organization.interfaces.rest.transform.OrganizationResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** The organization named by the {@code X-Organization-Id} header. */
@Path("/api/v1/organization")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Organizations", description = "Businesses the caller founded or joined")
public class CurrentOrganizationController {
    private final OrganizationCommandService organizationCommandService;
    private final OrganizationQueryService organizationQueryService;
    private final TenantContext tenantContext;

    public CurrentOrganizationController(OrganizationCommandService organizationCommandService,
                                         OrganizationQueryService organizationQueryService, TenantContext tenantContext) {
        this.organizationCommandService = organizationCommandService;
        this.organizationQueryService = organizationQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Get my organization", description = "The organization of the X-Organization-Id header, with its plan.")
    @APIResponse(responseCode = "200", description = "Organization found",
            content = @Content(schema = @Schema(implementation = OrganizationResource.class)))
    @APIResponse(responseCode = "400", description = "Missing or malformed X-Organization-Id")
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a member of the organization")
    public OrganizationResource getOrganization() {
        var id = tenantContext.organizationId();
        var organization = organizationQueryService.handle(new GetOrganizationByIdQuery(id))
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        return OrganizationResourceFromEntityAssembler.toResourceFromEntity(organization);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rename organization", description = "Owner only (§5).")
    @APIResponse(responseCode = "200", description = "Organization renamed",
            content = @Content(schema = @Schema(implementation = OrganizationResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is not the owner")
    public OrganizationResource rename(@Valid RenameOrganizationResource resource) {
        var command = new RenameOrganizationCommand(tenantContext.organizationId(), resource.name());
        return OrganizationResourceFromEntityAssembler.toResourceFromEntity(organizationCommandService.handle(command));
    }
}
