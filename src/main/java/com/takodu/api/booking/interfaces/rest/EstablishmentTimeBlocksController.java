package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.application.commandservices.TimeBlockCommandService;
import com.takodu.api.booking.application.queryservices.TimeBlockQueryService;
import com.takodu.api.booking.domain.model.queries.GetTimeBlocksByEstablishmentQuery;
import com.takodu.api.booking.interfaces.rest.resources.CreateTimeBlockResource;
import com.takodu.api.booking.interfaces.rest.resources.TimeBlockResource;
import com.takodu.api.booking.interfaces.rest.transform.CreateTimeBlockCommandFromResourceAssembler;
import com.takodu.api.booking.interfaces.rest.transform.TimeBlockResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import com.takodu.api.shared.interfaces.rest.transform.QueryParameters;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/establishments/{establishmentId}/time-blocks")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Time blocks", description = "Ranges in which a member or the whole establishment cannot be booked")
public class EstablishmentTimeBlocksController {
    private final TimeBlockCommandService timeBlockCommandService;
    private final TimeBlockQueryService timeBlockQueryService;
    private final TenantContext tenantContext;

    public EstablishmentTimeBlocksController(TimeBlockCommandService timeBlockCommandService,
                                             TimeBlockQueryService timeBlockQueryService, TenantContext tenantContext) {
        this.timeBlockCommandService = timeBlockCommandService;
        this.timeBlockQueryService = timeBlockQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List time blocks", description = "Blocks overlapping [from, to), at most 42 days.")
    @APIResponse(responseCode = "200", description = "Blocks, earliest first",
            content = @Content(schema = @Schema(implementation = TimeBlockResource.class)))
    @APIResponse(responseCode = "400", description = "from or to missing, to not after from, or more than 42 days")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public List<TimeBlockResource> getTimeBlocks(@PathParam("establishmentId") UUID establishmentId,
                                                 @QueryParam("from") String from, @QueryParam("to") String to) {
        var query = new GetTimeBlocksByEstablishmentQuery(tenantContext.organizationId(),
                new EstablishmentId(establishmentId),
                new TimeRange(QueryParameters.instant("from", from), QueryParameters.instant("to", to)));
        return timeBlockQueryService.handle(query).stream()
                .map(TimeBlockResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Create time block",
            description = "Any member blocks their own time; owners and managers block anyone or the whole establishment (B15). Works while the plan is inactive.")
    @APIResponse(responseCode = "201", description = "Block created, or the one an earlier request with the same key created",
            content = @Content(schema = @Schema(implementation = TimeBlockResource.class)))
    @APIResponse(responseCode = "400", description = "end not after start, longer than 366 days, or missing Idempotency-Key")
    @APIResponse(responseCode = "403", description = "Staff blocking someone else, the whole establishment, or an establishment they are not assigned to")
    @APIResponse(responseCode = "404", description = "Establishment or member not found in the organization")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    @APIResponse(responseCode = "422", description = "MEMBER_NOT_AT_ESTABLISHMENT (O13)")
    public TimeBlockResource create(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                    @PathParam("establishmentId") UUID establishmentId,
                                    @Valid CreateTimeBlockResource resource) {
        var command = CreateTimeBlockCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                tenantContext.memberId(), establishmentId, idempotencyKey, resource);
        return TimeBlockResourceFromEntityAssembler.toResourceFromEntity(timeBlockCommandService.handle(command));
    }
}
