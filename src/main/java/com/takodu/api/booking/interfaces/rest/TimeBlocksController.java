package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.application.commandservices.TimeBlockCommandService;
import com.takodu.api.booking.domain.model.commands.DeleteTimeBlockCommand;
import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.shared.application.tenancy.TenantContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

@Path("/api/v1/time-blocks")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Time blocks", description = "Ranges in which a member or the whole establishment cannot be booked")
public class TimeBlocksController {
    private final TimeBlockCommandService timeBlockCommandService;
    private final TenantContext tenantContext;

    public TimeBlocksController(TimeBlockCommandService timeBlockCommandService, TenantContext tenantContext) {
        this.timeBlockCommandService = timeBlockCommandService;
        this.tenantContext = tenantContext;
    }

    @DELETE
    @Path("/{timeBlockId}")
    @ResponseStatus(204)
    @Operation(summary = "Delete time block", description = "Any member deletes their own; owners and managers any (B15).")
    @APIResponse(responseCode = "204", description = "Block deleted")
    @APIResponse(responseCode = "403", description = "Staff deleting a block that is not their own")
    @APIResponse(responseCode = "404", description = "Block already deleted or not in the organization")
    public void delete(@PathParam("timeBlockId") UUID timeBlockId) {
        timeBlockCommandService.handle(new DeleteTimeBlockCommand(tenantContext.organizationId(),
                new TimeBlockId(timeBlockId), tenantContext.memberId()));
    }
}
