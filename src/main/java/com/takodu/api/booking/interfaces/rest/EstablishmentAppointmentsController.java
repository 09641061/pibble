package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.model.queries.GetAppointmentsByEstablishmentQuery;
import com.takodu.api.booking.interfaces.rest.resources.AppointmentResource;
import com.takodu.api.booking.interfaces.rest.transform.AppointmentResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import com.takodu.api.shared.interfaces.rest.transform.QueryParameters;
import jakarta.ws.rs.GET;
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

import java.util.List;
import java.util.UUID;

@Path("/api/v1/establishments/{establishmentId}/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Appointments", description = "Booking, moving, cancelling and following appointments")
public class EstablishmentAppointmentsController {
    private final AppointmentQueryService appointmentQueryService;
    private final TenantContext tenantContext;

    public EstablishmentAppointmentsController(AppointmentQueryService appointmentQueryService,
                                               TenantContext tenantContext) {
        this.appointmentQueryService = appointmentQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List the calendar",
            description = "Appointments in every status overlapping [from, to), at most 42 days; also those outside today's opening hours (O8).")
    @APIResponse(responseCode = "200", description = "Appointments, earliest first",
            content = @Content(schema = @Schema(implementation = AppointmentResource.class)))
    @APIResponse(responseCode = "400", description = "from or to missing, to not after from, or more than 42 days")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public List<AppointmentResource> getAppointments(@PathParam("establishmentId") UUID establishmentId,
                                                     @QueryParam("from") String from, @QueryParam("to") String to) {
        var query = new GetAppointmentsByEstablishmentQuery(tenantContext.organizationId(),
                new EstablishmentId(establishmentId),
                new TimeRange(QueryParameters.instant("from", from), QueryParameters.instant("to", to)));
        return appointmentQueryService.handle(query).stream()
                .map(AppointmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
