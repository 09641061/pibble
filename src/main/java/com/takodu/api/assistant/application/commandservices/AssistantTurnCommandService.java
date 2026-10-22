package com.takodu.api.assistant.application.commandservices;

import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;

import java.util.Optional;

public interface AssistantTurnCommandService {

    /**
     * A2: queues a turn for the customer's message when the assistant may answer now; a reaction is never answered.
     * Runs in the webhook's transaction and calls no model. Safe to call twice for one message.
     *
     * @return the queued turn, empty when the assistant stays quiet
     */
    Optional<AssistantTurn> handle(EnqueueAssistantTurnCommand command);
}
