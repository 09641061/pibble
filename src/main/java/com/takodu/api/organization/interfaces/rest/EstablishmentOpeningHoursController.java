package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.EstablishmentCommandService;
import com.takodu.api.organization.application.queryservices.EstablishmentQueryService;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentByIdQuery;
import com.takodu.api.organization.interfaces.rest.resources.OpeningHoursResource;
import com.takodu.api.organization.interfaces.rest.transform.OpeningHoursResourceFromEntityAssembler;
import com.takodu.api.organization.interfaces.rest.transform.ReplaceOpeningHoursCommandFromResourceAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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

@Path("/api/v1/establishments/{establishmentId}/opening-hours")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Establishments", description = "Branches of the caller's organization")
public class EstablishmentOpeningHoursController {
    private final EstablishmentCommandService establishmentCommandService;
    private final EstablishmentQueryService establishmentQueryService;
    private final TenantContext tenantContext;

    public EstablishmentOpeningHoursController(EstablishmentCommandService establishmentCommandService,
                                               EstablishmentQueryService establishmentQueryService,
                                               TenantContext tenantContext) {
        this.establishmentCommandService = establishmentCommandService;
        this.establishmentQueryService = establishmentQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Get opening hours")
    @APIResponse(responseCode = "200", description = "The week, by weekday and opening time",
            content = @Content(schema = @Schema(implementation = OpeningHoursResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public OpeningHoursResource getOpeningHours(@PathParam("establishmentId") UUID establishmentId) {
        var id = new EstablishmentId(establishmentId);
        var establishment = establishmentQueryService.handle(new GetEstablishmentByIdQuery(tenantContext.organizationId(), id))
                .orElseThrow(() -> new EstablishmentNotFoundException(id));
        return OpeningHoursResourceFromEntityAssembler.toResourceFromEntity(establishment);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Replace opening hours", description = "Owner or manager. Replaces the whole week; appointments already booked outside the new hours are kept.")
    @APIResponse(responseCode = "200", description = "Opening hours replaced",
            content = @Content(schema = @Schema(implementation = OpeningHoursResource.class)))
    @APIResponse(responseCode = "400", description = "Overlapping ranges, a range that crosses midnight, or a weekday outside 1-7")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    @APIResponse(responseCode = "422", description = "ESTABLISHMENT_ARCHIVED")
    public OpeningHoursResource replace(@PathParam("establishmentId") UUID establishmentId,
                                        @Valid OpeningHoursResource resource) {
        var command = ReplaceOpeningHoursCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(), establishmentId, resource);
        return OpeningHoursResourceFromEntityAssembler.toResourceFromEntity(establishmentCommandService.handle(command));
    }
}
