package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.queryservices.ConversationQueryService;
import com.takodu.api.conversations.domain.model.queries.GetConversationsByEstablishmentQuery;
import com.takodu.api.conversations.interfaces.rest.resources.ConversationPageResource;
import com.takodu.api.conversations.interfaces.rest.transform.ConversationResourceFromEntityAssembler;
import com.takodu.api.conversations.interfaces.rest.transform.PageCursorAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.ws.rs.DefaultValue;
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

import java.util.UUID;

@Path("/api/v1/establishments/{establishmentId}/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class EstablishmentConversationsController {
    private final ConversationQueryService conversationQueryService;
    private final TenantContext tenantContext;

    public EstablishmentConversationsController(ConversationQueryService conversationQueryService,
                                                TenantContext tenantContext) {
        this.conversationQueryService = conversationQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List the inbox", description = "Newest activity first, with unread counts, window and assistant state (W7).")
    @APIResponse(responseCode = "200", description = "One page",
            content = @Content(schema = @Schema(implementation = ConversationPageResource.class)))
    @APIResponse(responseCode = "400", description = "Invalid cursor or size")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not in the organization")
    public ConversationPageResource list(@PathParam("establishmentId") UUID establishmentId,
                                         @QueryParam("cursor") String cursor,
                                         @QueryParam("size") @DefaultValue("30") int size) {
        var query = new GetConversationsByEstablishmentQuery(tenantContext.organizationId(),
                new EstablishmentId(establishmentId), PageCursorAssembler.toCursorFromToken("cursor", cursor), size);
        return ConversationResourceFromEntityAssembler.toPageFromEntities(conversationQueryService.handle(query), size);
    }
}
