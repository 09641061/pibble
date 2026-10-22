package com.takodu.api.assistant.application.internal.outboundservices.llm;

/**
 * The language model behind the assistant, with the booking tools of the current turn. Any OpenAI-compatible
 * provider, chosen by configuration (08 §8).
 */
public interface AssistantModel {

    /**
     * Runs the model, which may call the tools of {@code TurnScope}'s turn before it answers.
     *
     * @throws AssistantModelException when the provider fails or answers something unusable
     */
    ModelAnswer answer(TurnPrompt prompt);
}
