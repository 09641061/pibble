package com.takodu.api.assistant.application.internal.eventhandlers;

import com.takodu.api.assistant.application.commandservices.AssistantTurnCommandService;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.interfaces.events.InboundMessageReceivedIntegrationEvent;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class InboundMessageReceivedEventHandler {
    private final AssistantTurnCommandService assistantTurnCommandService;

    public InboundMessageReceivedEventHandler(AssistantTurnCommandService assistantTurnCommandService) {
        this.assistantTurnCommandService = assistantTurnCommandService;
    }

    /** A2: queues a turn in the webhook's transaction, so a stored message is never left unanswered. */
    void on(@Observes InboundMessageReceivedIntegrationEvent event) {
        assistantTurnCommandService.handle(new EnqueueAssistantTurnCommand(new OrganizationId(event.organizationId()),
                new EstablishmentId(event.establishmentId()), new ConversationId(event.conversationId()),
                new CustomerId(event.customerId()), new MessageId(event.messageId()), event.kind(),
                event.occurredAt()));
    }
}
