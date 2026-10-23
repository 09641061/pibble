package com.takodu.api.assistant.application.internal.outboundservices.llm;

/** The provider failed or answered something unusable; the turn is tried again (A7). Never a business refusal. */
public class AssistantModelException extends RuntimeException {
    public AssistantModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
