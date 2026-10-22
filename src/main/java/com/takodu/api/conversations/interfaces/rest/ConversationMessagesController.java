package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.commandservices.MessageCommandService;
import com.takodu.api.conversations.application.queryservices.MessageQueryService;
import com.takodu.api.conversations.domain.model.commands.SendStaffMessageCommand;
import com.takodu.api.conversations.domain.model.queries.GetMessagesByConversationQuery;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.interfaces.rest.resources.MessagePageResource;
import com.takodu.api.conversations.interfaces.rest.resources.MessageResource;
import com.takodu.api.conversations.interfaces.rest.resources.SendMessageResource;
import com.takodu.api.conversations.interfaces.rest.transform.MessageResourceFromEntityAssembler;
import com.takodu.api.conversations.interfaces.rest.transform.PageCursorAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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

import java.util.UUID;

@Path("/api/v1/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class ConversationMessagesController {
    private final MessageCommandService messageCommandService;
    private final MessageQueryService messageQueryService;
    private final TenantContext tenantContext;

    public ConversationMessagesController(MessageCommandService messageCommandService,
                                          MessageQueryService messageQueryService, TenantContext tenantContext) {
        this.messageCommandService = messageCommandService;
        this.messageQueryService = messageQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Read the chat", description = "Newest first; pass nextCursor as before for older messages. Reading does not mark the chat read.")
    @APIResponse(responseCode = "200", description = "One page",
            content = @Content(schema = @Schema(implementation = MessagePageResource.class)))
    @APIResponse(responseCode = "400", description = "Invalid cursor or size")
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation not in the organization")
    public MessagePageResource list(@PathParam("conversationId") UUID conversationId,
                                    @QueryParam("before") String before,
                                    @QueryParam("size") @DefaultValue("50") int size) {
        var query = new GetMessagesByConversationQuery(tenantContext.organizationId(), new ConversationId(conversationId),
                PageCursorAssembler.toCursorFromToken("before", before), size);
        return MessageResourceFromEntityAssembler.toPageFromEntities(messageQueryService.handle(query), size);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Reply", description = "Queues a staff text inside the 24-hour window (W6) and pauses the assistant for 12 hours (A3). It is sent right after; its status follows.")
    @APIResponse(responseCode = "201", description = "Queued as pending, or the message an earlier request with the same key queued",
            content = @Content(schema = @Schema(implementation = MessageResource.class)))
    @APIResponse(responseCode = "400", description = "Blank or longer than 4,096 characters, or missing Idempotency-Key")
    @APIResponse(responseCode = "402", description = "Plan inactive (O6)")
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation not in the organization")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    @APIResponse(responseCode = "422", description = "WINDOW_CLOSED (send a template) or LINE_DISCONNECTED")
    public MessageResource send(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                @PathParam("conversationId") UUID conversationId,
                                @Valid SendMessageResource resource) {
        var command = new SendStaffMessageCommand(tenantContext.organizationId(), new ConversationId(conversationId),
                tenantContext.memberId(), resource == null ? null : resource.body(), idempotencyKey == null ? null : new IdempotencyKey(idempotencyKey));
        return MessageResourceFromEntityAssembler.toResourceFromEntity(messageCommandService.handle(command));
    }
}
