package com.takodu.api.assistant.application.internal.eventhandlers;

import com.takodu.api.assistant.application.commandservices.AssistantTurnCommandService;
import com.takodu.api.assistant.application.internal.jobs.TurnWorker;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.interfaces.events.InboundMessageReceivedIntegrationEvent;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@ApplicationScoped
public class InboundMessageReceivedEventHandler {
    private final AssistantTurnCommandService assistantTurnCommandService;
    private final TurnWorker turnWorker;

    public InboundMessageReceivedEventHandler(AssistantTurnCommandService assistantTurnCommandService,
                                              TurnWorker turnWorker) {
        this.assistantTurnCommandService = assistantTurnCommandService;
        this.turnWorker = turnWorker;
    }

    /** A2: queues a turn in the webhook's transaction, so a stored message is never left unanswered. */
    void on(@Observes InboundMessageReceivedIntegrationEvent event) {
        assistantTurnCommandService.handle(new EnqueueAssistantTurnCommand(new OrganizationId(event.organizationId()),
                new EstablishmentId(event.establishmentId()), new ConversationId(event.conversationId()),
                new CustomerId(event.customerId()), new MessageId(event.messageId()), event.kind(),
                event.occurredAt()));
    }

    /** Only a wake-up: if it is lost, the worker's schedule finds the turn. */
    void wake(@Observes(during = TransactionPhase.AFTER_SUCCESS) InboundMessageReceivedIntegrationEvent event) {
        turnWorker.wake();
    }
}
