package com.takodu.api.assistant.infrastructure.llm;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

/** The LangChain4j AI service of one turn: the system prompt, the chat memory and the tools are set when built. */
public interface BookingAgent {
    Result<AgentReply> reply(@UserMessage String customerMessage);
}
