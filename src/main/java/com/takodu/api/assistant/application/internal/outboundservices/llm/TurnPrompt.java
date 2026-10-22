package com.takodu.api.assistant.application.internal.outboundservices.llm;

import com.takodu.api.assistant.domain.model.valueobjects.ChatLine;

import java.util.List;

/**
 * What the model is told for one turn. Never the customer's phone number (08 §7).
 *
 * @param today the establishment's local date, in Spanish, such as {@code jueves 22 de octubre de 2026}
 * @param history the chat before the customer's latest messages, oldest first
 * @param customerMessage the customer's latest messages, joined; what the model answers
 */
public record TurnPrompt(String establishmentName, String today, String businessInfo, List<ChatLine> history,
                         String customerMessage) {
    public TurnPrompt {
        history = List.copyOf(history);
    }
}
