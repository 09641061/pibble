package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.AppointmentNotUpcomingException;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.CustomerDeletedException;
import com.takodu.api.conversations.domain.exceptions.LineDisconnectedException;
import com.takodu.api.conversations.domain.exceptions.TemplateNotApprovedException;
import com.takodu.api.conversations.domain.exceptions.WindowClosedException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.commands.SendAssistantReplyCommand;
import com.takodu.api.conversations.domain.model.commands.SendNextPendingMessageCommand;
import com.takodu.api.conversations.domain.model.commands.SendStaffMessageCommand;
import com.takodu.api.conversations.domain.model.commands.SendStaffTemplateCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

import java.util.Optional;

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
     * W6, A3, 07 §7: queues the reminder or confirmation of one of the customer's upcoming appointments, so staff
     * reach a customer whose window is closed. It is rendered as the dispatcher renders it: the approved template
     * outside the window (and always for a reminder), the same text inside. Like a staff reply it pauses the
     * assistant and clears unread.
     *
     * @return the pending message, or the one an earlier request with the same key queued
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     * @throws PlanInactiveException when the plan is inactive (O6)
     * @throws LineDisconnectedException when the line is disconnected (W15)
     * @throws AppointmentNotUpcomingException when the appointment is not one of the customer's upcoming confirmed
     *         appointments at the conversation's establishment
     * @throws TemplateNotApprovedException when it must go as a template Meta has not approved on the line
     * @throws CustomerDeletedException when the customer was deleted (C5)
     */
    Message handle(SendStaffTemplateCommand command);

    /**
     * A2, A8: queues the assistant's reply in the outbox. It is dropped when the assistant may no longer answer: a
     * person wrote meanwhile or it was handed off (A3, A7), the window closed (W6), the line disconnected (W15) or the
     * plan lapsed (O6). Runs without a caller, for the assistant's turn.
     *
     * @return the pending message, or the one an earlier turn attempt queued; empty when dropped
     * @throws ConversationNotFoundException when the conversation is not in the organization
     */
    Optional<Message> handle(SendAssistantReplyCommand command);

    /**
     * W14, W15: sends the oldest due outbound message, re-checking the rules that allowed it, and records the result
     * on the message. Runs without a caller, for the sender job.
     *
     * @return whether a message was due; false when the outbox has nothing to send now
     */
    boolean handle(SendNextPendingMessageCommand command);
}
