package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.commandservices.MessageCommandService;
import com.takodu.api.conversations.domain.model.commands.SendStaffTemplateCommand;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.conversations.interfaces.rest.resources.MessageResource;
import com.takodu.api.conversations.interfaces.rest.resources.SendTemplateResource;
import com.takodu.api.conversations.interfaces.rest.transform.MessageResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
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

import java.util.Locale;
import java.util.UUID;

@Path("/api/v1/conversations/{conversationId}/templates")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class ConversationTemplatesController {
    private final MessageCommandService messageCommandService;
    private final TenantContext tenantContext;

    public ConversationTemplatesController(MessageCommandService messageCommandService, TenantContext tenantContext) {
        this.messageCommandService = messageCommandService;
        this.tenantContext = tenantContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Send an appointment template", description = "Queues the reminder or confirmation of one of the customer's upcoming appointments, which reaches them even when the 24-hour window is closed (W6). Outside the window, and always for a reminder, it goes as the approved template; inside, as the same text. Pauses the assistant for 12 hours like a reply (A3).")
    @APIResponse(responseCode = "201", description = "Queued as pending, or the message an earlier request with the same key queued",
            content = @Content(schema = @Schema(implementation = MessageResource.class)))
    @APIResponse(responseCode = "400", description = "Template not reminder or confirmation, no appointment, or missing Idempotency-Key")
    @APIResponse(responseCode = "402", description = "Plan inactive (O6)")
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Conversation not in the organization")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    @APIResponse(responseCode = "422", description = "LINE_DISCONNECTED, TEMPLATE_NOT_APPROVED, APPOINTMENT_NOT_UPCOMING or CUSTOMER_DELETED")
    public MessageResource send(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                @PathParam("conversationId") UUID conversationId,
                                @Valid @NotNull SendTemplateResource resource) {
        var command = new SendStaffTemplateCommand(tenantContext.organizationId(), new ConversationId(conversationId),
                tenantContext.memberId(), NotificationKind.valueOf(resource.template().toUpperCase(Locale.ROOT)),
                new AppointmentId(resource.appointmentId()),
                idempotencyKey == null ? null : new IdempotencyKey(idempotencyKey));
        return MessageResourceFromEntityAssembler.toResourceFromEntity(messageCommandService.handle(command));
    }
}
