package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.ServiceCommandService;
import com.takodu.api.organization.application.queryservices.ServiceQueryService;
import com.takodu.api.organization.domain.model.queries.GetServicesByEstablishmentQuery;
import com.takodu.api.organization.interfaces.rest.resources.ServiceDetailsResource;
import com.takodu.api.organization.interfaces.rest.resources.ServiceResource;
import com.takodu.api.organization.interfaces.rest.transform.ServiceCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.ServiceResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.validation.Valid;
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

@Path("/api/v1/establishments/{establishmentId}/services")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Services", description = "What each establishment sells and who performs it")
public class EstablishmentServicesController {
    private final ServiceCommandService serviceCommandService;
    private final ServiceQueryService serviceQueryService;
    private final TenantContext tenantContext;

    public EstablishmentServicesController(ServiceCommandService serviceCommandService,
                                           ServiceQueryService serviceQueryService, TenantContext tenantContext) {
        this.serviceCommandService = serviceCommandService;
        this.serviceQueryService = serviceQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List services of an establishment", description = "Active and inactive services.")
    @APIResponse(responseCode = "200", description = "Services by name",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public List<ServiceResource> getServices(@PathParam("establishmentId") UUID establishmentId) {
        var query = new GetServicesByEstablishmentQuery(tenantContext.organizationId(), new EstablishmentId(establishmentId));
        return serviceQueryService.handle(query).stream()
                .map(ServiceResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Create service", description = "Owner or manager (O14, O15).")
    @APIResponse(responseCode = "201", description = "Service created, or the one an earlier request with the same key created",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name, duration or price out of range, or missing Idempotency-Key")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Establishment or performer not found in the organization")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    @APIResponse(responseCode = "422", description = "ESTABLISHMENT_ARCHIVED, or PERFORMER_NOT_ELIGIBLE (O15)")
    public ServiceResource create(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                  @PathParam("establishmentId") UUID establishmentId,
                                  @Valid ServiceDetailsResource resource) {
        var command = ServiceCommandFromResourceAssembler.toCreateCommandFromResource(tenantContext.organizationId(),
                establishmentId, idempotencyKey, resource);
        return ServiceResourceFromEntityAssembler.toResourceFromEntity(serviceCommandService.handle(command));
    }
}
