package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.commandservices.ConversationCommandService;
import com.takodu.api.conversations.application.queryservices.ConversationQueryService;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.model.commands.AssignConversationCommand;
import com.takodu.api.conversations.domain.model.commands.MarkConversationReadCommand;
import com.takodu.api.conversations.domain.model.queries.GetConversationByIdQuery;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.interfaces.rest.resources.AssignConversationResource;
import com.takodu.api.conversations.interfaces.rest.resources.ConversationResource;
import com.takodu.api.conversations.interfaces.rest.transform.ConversationResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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

import java.util.UUID;

@Path("/api/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class ConversationsController {
    private final ConversationCommandService conversationCommandService;
    private final ConversationQueryService conversationQueryService;
    private final TenantContext tenantContext;

    public ConversationsController(ConversationCommandService conversationCommandService,
                                   ConversationQueryService conversationQueryService, TenantContext tenantContext) {
        this.conversationCommandService = conversationCommandService;
        this.conversationQueryService = conversationQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Path("/{conversationId}")
    @Operation(summary = "Get a conversation")
    @APIResponse(responseCode = "200", description = "The conversation",
            content = @Content(schema = @Schema(implementation = ConversationResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation not in the organization")
    public ConversationResource get(@PathParam("conversationId") UUID conversationId) {
        var id = new ConversationId(conversationId);
        return conversationQueryService.handle(new GetConversationByIdQuery(tenantContext.organizationId(), id))
                .map(ConversationResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new ConversationNotFoundException(id));
    }

    @POST
    @Path("/{conversationId}/read")
    @WithoutIdempotencyKey("Marking a conversation read twice leaves it read")
    @Operation(summary = "Mark read", description = "Resets the unread count (W8). Works while the plan is inactive.")
    @APIResponse(responseCode = "200", description = "The conversation",
            content = @Content(schema = @Schema(implementation = ConversationResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation not in the organization")
    public ConversationResource markRead(@PathParam("conversationId") UUID conversationId) {
        var id = new ConversationId(conversationId);
        conversationCommandService.handle(new MarkConversationReadCommand(tenantContext.organizationId(), id));
        return get(conversationId);
    }

    @PUT
    @Path("/{conversationId}/assignee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Assign", description = "Informative only (W9); any member who sees the inbox may be assigned.")
    @APIResponse(responseCode = "200", description = "The conversation",
            content = @Content(schema = @Schema(implementation = ConversationResource.class)))
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation or member not in the organization")
    @APIResponse(responseCode = "422", description = "MEMBER_NOT_AT_ESTABLISHMENT")
    public ConversationResource assign(@PathParam("conversationId") UUID conversationId,
                                       @Valid AssignConversationResource resource) {
        var memberId = resource == null || resource.memberId() == null ? null : new MemberId(resource.memberId());
        conversationCommandService.handle(new AssignConversationCommand(tenantContext.organizationId(),
                new ConversationId(conversationId), memberId));
        return get(conversationId);
    }
}
