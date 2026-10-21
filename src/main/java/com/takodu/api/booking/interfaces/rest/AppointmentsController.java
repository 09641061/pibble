package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.application.commandservices.AppointmentCommandService;
import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.exceptions.AppointmentNotFoundException;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.interfaces.rest.resources.AppointmentResource;
import com.takodu.api.booking.interfaces.rest.resources.BookAppointmentResource;
import com.takodu.api.booking.interfaces.rest.transform.AppointmentResourceFromEntityAssembler;
import com.takodu.api.booking.interfaces.rest.transform.BookAppointmentCommandFromResourceAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
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

import java.util.UUID;

@Path("/api/v1/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Appointments", description = "Booking, moving, cancelling and following appointments")
public class AppointmentsController {
    private final AppointmentCommandService appointmentCommandService;
    private final AppointmentQueryService appointmentQueryService;
    private final TenantContext tenantContext;

    public AppointmentsController(AppointmentCommandService appointmentCommandService,
                                  AppointmentQueryService appointmentQueryService, TenantContext tenantContext) {
        this.appointmentCommandService = appointmentCommandService;
        this.appointmentQueryService = appointmentQueryService;
        this.tenantContext = tenantContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Book appointment",
            description = "Any member, in the establishments they see (B1-B9). The price is copied from the service (B8).")
    @APIResponse(responseCode = "201", description = "Appointment booked, or the one an earlier request with the same key booked",
            content = @Content(schema = @Schema(implementation = AppointmentResource.class)))
    @APIResponse(responseCode = "400", description = "Missing field or Idempotency-Key")
    @APIResponse(responseCode = "402", description = "The plan is not active (B7)")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the service's establishment")
    @APIResponse(responseCode = "404", description = "Service or customer not found in the organization, or the customer is deleted")
    @APIResponse(responseCode = "409", description = "SLOT_UNAVAILABLE: the member or the customer is busy then (B5, B6); or the same request is still running")
    @APIResponse(responseCode = "422", description = "SERVICE_NOT_BOOKABLE (B1), MEMBER_CANNOT_PERFORM_SERVICE (B2), OUTSIDE_OPENING_HOURS (B3), TIME_BLOCKED (B4) or OUTSIDE_BOOKING_WINDOW (B9)")
    public AppointmentResource book(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                    @Valid BookAppointmentResource resource) {
        var command = BookAppointmentCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                tenantContext.userId(), idempotencyKey, resource);
        return AppointmentResourceFromEntityAssembler.toResourceFromEntity(appointmentCommandService.handle(command));
    }

    @GET
    @Path("/{appointmentId}")
    @Operation(summary = "Get appointment by id")
    @APIResponse(responseCode = "200", description = "Appointment found",
            content = @Content(schema = @Schema(implementation = AppointmentResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the appointment's establishment")
    @APIResponse(responseCode = "404", description = "Appointment not found in the organization")
    public AppointmentResource getAppointmentById(@PathParam("appointmentId") UUID appointmentId) {
        var id = new AppointmentId(appointmentId);
        var appointment = appointmentQueryService.handle(new GetAppointmentByIdQuery(tenantContext.organizationId(), id))
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return AppointmentResourceFromEntityAssembler.toResourceFromEntity(appointment);
    }
}
