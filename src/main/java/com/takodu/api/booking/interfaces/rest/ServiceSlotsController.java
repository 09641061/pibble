package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.model.queries.GetAvailableSlotsQuery;
import com.takodu.api.booking.interfaces.rest.resources.SlotResource;
import com.takodu.api.booking.interfaces.rest.transform.SlotResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
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

@Path("/api/v1/services/{serviceId}/slots")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Appointments", description = "Booking, moving, cancelling and following appointments")
public class ServiceSlotsController {
    private final AppointmentQueryService appointmentQueryService;
    private final TenantContext tenantContext;

    public ServiceSlotsController(AppointmentQueryService appointmentQueryService, TenantContext tenantContext) {
        this.appointmentQueryService = appointmentQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List free slots",
            description = "Start times every 15 minutes on a date of the establishment's time zone, from now on, with who is free (B16).")
    @APIResponse(responseCode = "200", description = "Slots, earliest first",
            content = @Content(schema = @Schema(implementation = SlotResource.class)))
    @APIResponse(responseCode = "400", description = "date missing or not yyyy-MM-dd")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the service's establishment")
    @APIResponse(responseCode = "404", description = "Service or customer not found in the organization")
    @APIResponse(responseCode = "422", description = "SERVICE_NOT_BOOKABLE: inactive service or archived establishment (B1)")
    public List<SlotResource> getSlots(@PathParam("serviceId") UUID serviceId,
                                       @QueryParam("date") String date,
                                       @QueryParam("staffMemberId") UUID staffMemberId,
                                       @QueryParam("customerId") UUID customerId) {
        var query = new GetAvailableSlotsQuery(tenantContext.organizationId(), new ServiceId(serviceId),
                QueryParameters.date("date", date),
                staffMemberId == null ? null : new MemberId(staffMemberId),
                customerId == null ? null : new CustomerId(customerId));
        return appointmentQueryService.handle(query).stream()
                .map(SlotResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
