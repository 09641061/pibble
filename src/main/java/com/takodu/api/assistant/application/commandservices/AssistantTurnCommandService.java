package com.takodu.api.assistant.application.commandservices;

import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;
import com.takodu.api.assistant.domain.model.commands.RunNextTurnCommand;

import java.util.Optional;

public interface AssistantTurnCommandService {

    /**
     * A2: queues a turn for the customer's message when the assistant may answer now; a reaction is never answered.
     * Runs in the webhook's transaction and calls no model. Safe to call twice for one message.
     *
     * @return the queued turn, empty when the assistant stays quiet
     */
    Optional<AssistantTurn> handle(EnqueueAssistantTurnCommand command);

    /**
     * Runs the next due turn: re-checks A2, lets the model answer with the booking tools, queues the reply and hands
     * off when needed (A7). The model runs outside any transaction; claiming and recording the outcome each take a
     * short one. Runs without a caller, for {@code TurnWorker}.
     *
     * @return whether a turn was due; false when none is
     */
    boolean handle(RunNextTurnCommand command);
}
