package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.commandservices.WhatsappLineCommandService;
import com.takodu.api.conversations.application.queryservices.WhatsappLineQueryService;
import com.takodu.api.conversations.domain.exceptions.WhatsappLineNotFoundException;
import com.takodu.api.conversations.domain.model.commands.DisconnectWhatsappLineCommand;
import com.takodu.api.conversations.domain.model.queries.GetWhatsappLineByEstablishmentQuery;
import com.takodu.api.conversations.interfaces.rest.resources.ConnectWhatsappLineResource;
import com.takodu.api.conversations.interfaces.rest.resources.WhatsappLineResource;
import com.takodu.api.conversations.interfaces.rest.transform.ConnectWhatsappLineCommandFromResourceAssembler;
import com.takodu.api.conversations.interfaces.rest.transform.WhatsappLineResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/api/v1/establishments/{establishmentId}/whatsapp-line")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "WhatsApp lines", description = "The WhatsApp business number of each establishment")
public class EstablishmentWhatsappLineController {
    private final WhatsappLineCommandService whatsappLineCommandService;
    private final WhatsappLineQueryService whatsappLineQueryService;
    private final TenantContext tenantContext;

    public EstablishmentWhatsappLineController(WhatsappLineCommandService whatsappLineCommandService,
                                               WhatsappLineQueryService whatsappLineQueryService,
                                               TenantContext tenantContext) {
        this.whatsappLineCommandService = whatsappLineCommandService;
        this.whatsappLineQueryService = whatsappLineQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Get the line", description = "The connected line, else the last one, so the app can show it disconnected.")
    @APIResponse(responseCode = "200", description = "The line",
            content = @Content(schema = @Schema(implementation = WhatsappLineResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not in the organization, or it never had a line")
    public WhatsappLineResource get(@PathParam("establishmentId") UUID establishmentId) {
        var id = new EstablishmentId(establishmentId);
        return whatsappLineQueryService.handle(new GetWhatsappLineByEstablishmentQuery(tenantContext.organizationId(), id))
                .map(WhatsappLineResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new WhatsappLineNotFoundException(id));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Connect the line",
            description = "Owner only. Finishes Embedded Signup with Meta while the owner waits (W1, W2).")
    @APIResponse(responseCode = "201", description = "Line connected, or the one an earlier request with the same key connected",
            content = @Content(schema = @Schema(implementation = WhatsappLineResource.class)))
    @APIResponse(responseCode = "400", description = "Missing field, cloud_api without a 6-digit pin, or missing Idempotency-Key")
    @APIResponse(responseCode = "402", description = "Plan inactive (O6)")
    @APIResponse(responseCode = "403", description = "Caller is not the owner")
    @APIResponse(responseCode = "404", description = "Establishment not in the organization")
    @APIResponse(responseCode = "409", description = "LINE_ALREADY_CONNECTED: the establishment has a line or the number is on another one (W1)")
    @APIResponse(responseCode = "422", description = "WHATSAPP_SIGNUP_FAILED or ESTABLISHMENT_ARCHIVED")
    public WhatsappLineResource connect(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                        @PathParam("establishmentId") UUID establishmentId,
                                        @Valid ConnectWhatsappLineResource resource) {
        var command = ConnectWhatsappLineCommandFromResourceAssembler.toCommandFromResource(
                tenantContext.organizationId(), establishmentId, idempotencyKey, resource);
        return WhatsappLineResourceFromEntityAssembler.toResourceFromEntity(whatsappLineCommandService.handle(command));
    }

    @DELETE
    @ResponseStatus(204)
    @Operation(summary = "Disconnect the line", description = "Owner only. Conversations stay; new webhooks for the number are ignored.")
    @APIResponse(responseCode = "204", description = "Line disconnected")
    @APIResponse(responseCode = "403", description = "Caller is not the owner")
    @APIResponse(responseCode = "404", description = "Establishment not in the organization, or no connected line")
    public void disconnect(@PathParam("establishmentId") UUID establishmentId) {
        whatsappLineCommandService.handle(new DisconnectWhatsappLineCommand(tenantContext.organizationId(),
                new EstablishmentId(establishmentId)));
    }
}
