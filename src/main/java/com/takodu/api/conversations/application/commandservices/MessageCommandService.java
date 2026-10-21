package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.LineDisconnectedException;
import com.takodu.api.conversations.domain.exceptions.WindowClosedException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.commands.SendNextPendingMessageCommand;
import com.takodu.api.conversations.domain.model.commands.SendStaffMessageCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

public interface MessageCommandService {

    /**
     * W6, W7, A3: queues a staff reply in the outbox, pauses the assistant and clears unread. Meta is called later,
     * by the sender job.
     *
     * @return the pending message, or the one an earlier request with the same key queued
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     * @throws PlanInactiveException when the plan is inactive (O6)
     * @throws LineDisconnectedException when the line is disconnected (W15)
     * @throws WindowClosedException when the customer last wrote more than 24 hours ago (W6)
     */
    Message handle(SendStaffMessageCommand command);

    /**
     * W14, W15: sends the oldest due outbound message, re-checking the rules that allowed it, and records the result
     * on the message. Runs without a caller, for the sender job.
     *
     * @return whether a message was due; false when the outbox has nothing to send now
     */
    boolean handle(SendNextPendingMessageCommand command);
}
