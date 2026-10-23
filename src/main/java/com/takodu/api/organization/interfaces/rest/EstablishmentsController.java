package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.EstablishmentCommandService;
import com.takodu.api.organization.application.queryservices.EstablishmentQueryService;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.model.commands.ArchiveEstablishmentCommand;
import com.takodu.api.organization.domain.model.queries.GetActiveEstablishmentsQuery;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentByIdQuery;
import com.takodu.api.organization.interfaces.rest.resources.EstablishmentDetailsResource;
import com.takodu.api.organization.interfaces.rest.resources.EstablishmentResource;
import com.takodu.api.organization.interfaces.rest.transform.EstablishmentCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.EstablishmentResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/establishments")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Establishments", description = "Branches of the caller's organization")
public class EstablishmentsController {
    private final EstablishmentCommandService establishmentCommandService;
    private final EstablishmentQueryService establishmentQueryService;
    private final TenantContext tenantContext;

    public EstablishmentsController(EstablishmentCommandService establishmentCommandService,
                                    EstablishmentQueryService establishmentQueryService, TenantContext tenantContext) {
        this.establishmentCommandService = establishmentCommandService;
        this.establishmentQueryService = establishmentQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List establishments", description = "Active establishments the caller sees: all for owners and managers, assigned ones for staff (O13).")
    @APIResponse(responseCode = "200", description = "Establishments by name",
            content = @Content(schema = @Schema(implementation = EstablishmentResource.class)))
    public List<EstablishmentResource> getEstablishments() {
        return establishmentQueryService.handle(new GetActiveEstablishmentsQuery(tenantContext.organizationId())).stream()
                .map(EstablishmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @GET
    @Path("/{establishmentId}")
    @Operation(summary = "Get establishment by id")
    @APIResponse(responseCode = "200", description = "Establishment found, archived or not",
            content = @Content(schema = @Schema(implementation = EstablishmentResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public EstablishmentResource getEstablishmentById(@PathParam("establishmentId") UUID establishmentId) {
        var id = new EstablishmentId(establishmentId);
        var establishment = establishmentQueryService.handle(new GetEstablishmentByIdQuery(tenantContext.organizationId(), id))
                .orElseThrow(() -> new EstablishmentNotFoundException(id));
        return EstablishmentResourceFromEntityAssembler.toResourceFromEntity(establishment);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Open establishment", description = "Owner only. The new branch is closed until its opening hours are set.")
    @APIResponse(responseCode = "201", description = "Establishment opened, or the one an earlier request with the same key opened",
            content = @Content(schema = @Schema(implementation = EstablishmentResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name, unknown time zone, or missing Idempotency-Key")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is not the owner")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    @APIResponse(responseCode = "422", description = "PLAN_LIMIT_REACHED: the plan allows no more active establishments (O4)")
    public EstablishmentResource open(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                      @Valid EstablishmentDetailsResource resource) {
        var command = EstablishmentCommandFromResourceAssembler.toOpenCommandFromResource(
                tenantContext.organizationId(), idempotencyKey, resource);
        return EstablishmentResourceFromEntityAssembler.toResourceFromEntity(establishmentCommandService.handle(command));
    }

    @PUT
    @Path("/{establishmentId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Change establishment details", description = "Owner only. Name, address and time zone.")
    @APIResponse(responseCode = "200", description = "Establishment changed",
            content = @Content(schema = @Schema(implementation = EstablishmentResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name or unknown time zone")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is not the owner")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    @APIResponse(responseCode = "422", description = "ESTABLISHMENT_ARCHIVED")
    public EstablishmentResource describe(@PathParam("establishmentId") UUID establishmentId,
                                          @Valid EstablishmentDetailsResource resource) {
        var command = EstablishmentCommandFromResourceAssembler.toDescribeCommandFromResource(
                tenantContext.organizationId(), establishmentId, resource);
        return EstablishmentResourceFromEntityAssembler.toResourceFromEntity(establishmentCommandService.handle(command));
    }

    @POST
    @Path("/{establishmentId}/archivals")
    @WithoutIdempotencyKey("archiving an archived establishment keeps it archived")
    @Operation(summary = "Archive establishment", description = "Owner only. Frees its place in the plan (O4); history is kept.")
    @APIResponse(responseCode = "200", description = "Establishment archived",
            content = @Content(schema = @Schema(implementation = EstablishmentResource.class)))
    @APIResponse(responseCode = "403", description = "The caller is not the owner")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public EstablishmentResource archive(@PathParam("establishmentId") UUID establishmentId) {
        var command = new ArchiveEstablishmentCommand(tenantContext.organizationId(), new EstablishmentId(establishmentId));
        return EstablishmentResourceFromEntityAssembler.toResourceFromEntity(establishmentCommandService.handle(command));
    }
}
