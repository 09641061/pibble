package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.queryservices.ConversationQueryService;
import com.takodu.api.conversations.domain.model.queries.GetInboxSummaryQuery;
import com.takodu.api.conversations.interfaces.rest.resources.InboxSummaryResource;
import com.takodu.api.conversations.interfaces.rest.transform.InboxSummaryResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/inbox/summary")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class InboxSummaryController {
    private final ConversationQueryService conversationQueryService;
    private final TenantContext tenantContext;

    public InboxSummaryController(ConversationQueryService conversationQueryService, TenantContext tenantContext) {
        this.conversationQueryService = conversationQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Count the chats that need someone",
            description = "For the navigation badge: chats awaiting a person (handed off, or unread with nobody writing after the customer) and chats with unread messages, across the whole inbox of the establishment.")
    @APIResponse(responseCode = "200", description = "The counts",
            content = @Content(schema = @Schema(implementation = InboxSummaryResource.class)))
    @APIResponse(responseCode = "400", description = "establishmentId missing")
    @APIResponse(responseCode = "403", description = "Staff not assigned to the establishment")
    @APIResponse(responseCode = "404", description = "Establishment not in the organization")
    public InboxSummaryResource summary(@QueryParam("establishmentId") UUID establishmentId) {
        var query = new GetInboxSummaryQuery(tenantContext.organizationId(),
                establishmentId == null ? null : new EstablishmentId(establishmentId));
        return InboxSummaryResourceFromEntityAssembler.toResourceFromEntity(conversationQueryService.handle(query));
    }
}
