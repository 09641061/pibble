package com.takodu.api.assistant.domain.services;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationState;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;

import java.util.Optional;

/** A2: whether the assistant may answer a chat now. Checked when a message arrives and again before answering. */
public final class AnswerPolicy {
    private AnswerPolicy() { }

    /**
     * @param settings null when nobody configured the establishment's assistant
     * @return why it must stay quiet, or empty when it may answer
     */
    public static Optional<String> whyQuiet(AssistantSettings settings, EstablishmentProfile establishment,
                                            ConversationState conversation) {
        if (settings == null || !settings.isEnabled()) return Optional.of("The assistant is off");
        if (establishment.archived()) return Optional.of("The establishment is archived");
        if (!establishment.planActive()) return Optional.of("The plan is inactive");
        if (!conversation.lineConnected()) return Optional.of("The line is disconnected");
        if (!conversation.windowOpen()) return Optional.of("The customer service window is closed");
        if (conversation.paused()) return Optional.of("A person is handling the chat");
        return Optional.empty();
    }
}
