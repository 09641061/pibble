package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.ServiceCommandService;
import com.takodu.api.organization.application.queryservices.ServiceQueryService;
import com.takodu.api.organization.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.organization.domain.model.commands.ActivateServiceCommand;
import com.takodu.api.organization.domain.model.commands.DeactivateServiceCommand;
import com.takodu.api.organization.domain.model.queries.GetServiceByIdQuery;
import com.takodu.api.organization.interfaces.rest.resources.ServiceDetailsResource;
import com.takodu.api.organization.interfaces.rest.resources.ServiceResource;
import com.takodu.api.organization.interfaces.rest.transform.ServiceCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.ServiceResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/services")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Services", description = "What each establishment sells and who performs it")
public class ServicesController {
    private final ServiceCommandService serviceCommandService;
    private final ServiceQueryService serviceQueryService;
    private final TenantContext tenantContext;

    public ServicesController(ServiceCommandService serviceCommandService, ServiceQueryService serviceQueryService,
                              TenantContext tenantContext) {
        this.serviceCommandService = serviceCommandService;
        this.serviceQueryService = serviceQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Path("/{serviceId}")
    @Operation(summary = "Get service by id")
    @APIResponse(responseCode = "200", description = "Service found",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the service's establishment")
    @APIResponse(responseCode = "404", description = "Service not found in the organization")
    public ServiceResource getServiceById(@PathParam("serviceId") UUID serviceId) {
        var id = new ServiceId(serviceId);
        var service = serviceQueryService.handle(new GetServiceByIdQuery(tenantContext.organizationId(), id))
                .orElseThrow(() -> new ServiceNotFoundException(id));
        return ServiceResourceFromEntityAssembler.toResourceFromEntity(service);
    }

    @PUT
    @Path("/{serviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update service", description = "Owner or manager. Booked appointments keep their price (B8).")
    @APIResponse(responseCode = "200", description = "Service updated",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name, or duration or price out of range")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Service or performer not found in the organization")
    @APIResponse(responseCode = "422", description = "PERFORMER_NOT_ELIGIBLE (O15)")
    public ServiceResource update(@PathParam("serviceId") UUID serviceId, @Valid ServiceDetailsResource resource) {
        var command = ServiceCommandFromResourceAssembler.toUpdateCommandFromResource(tenantContext.organizationId(),
                serviceId, resource);
        return ServiceResourceFromEntityAssembler.toResourceFromEntity(serviceCommandService.handle(command));
    }

    @POST
    @Path("/{serviceId}/deactivations")
    @WithoutIdempotencyKey("deactivating an inactive service keeps it inactive")
    @Operation(summary = "Deactivate service", description = "Owner or manager. The service is no longer offered (O14).")
    @APIResponse(responseCode = "200", description = "Service deactivated",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Service not found in the organization")
    public ServiceResource deactivate(@PathParam("serviceId") UUID serviceId) {
        var command = new DeactivateServiceCommand(tenantContext.organizationId(), new ServiceId(serviceId));
        return ServiceResourceFromEntityAssembler.toResourceFromEntity(serviceCommandService.handle(command));
    }

    @POST
    @Path("/{serviceId}/activations")
    @WithoutIdempotencyKey("activating an active service keeps it active")
    @Operation(summary = "Activate service", description = "Owner or manager. Offers a deactivated service again.")
    @APIResponse(responseCode = "200", description = "Service activated",
            content = @Content(schema = @Schema(implementation = ServiceResource.class)))
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Service not found in the organization")
    public ServiceResource activate(@PathParam("serviceId") UUID serviceId) {
        var command = new ActivateServiceCommand(tenantContext.organizationId(), new ServiceId(serviceId));
        return ServiceResourceFromEntityAssembler.toResourceFromEntity(serviceCommandService.handle(command));
    }
}
