package com.takodu.api.assistant.application.internal.outboundservices.llm;

import com.takodu.api.assistant.domain.model.valueobjects.AssistantReply;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;

/**
 * @param usage what the turn cost, all model calls together (A9)
 * @param toolCalls how many tools the model ran
 */
public record ModelAnswer(AssistantReply reply, TurnUsage usage, int toolCalls) { }
