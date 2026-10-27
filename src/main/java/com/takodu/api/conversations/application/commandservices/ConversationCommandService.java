package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.MemberNotAtEstablishmentException;
import com.takodu.api.conversations.domain.exceptions.MemberNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.commands.AssignConversationCommand;
import com.takodu.api.conversations.domain.model.commands.HandOffConversationCommand;
import com.takodu.api.conversations.domain.model.commands.ReportAssistantActivityCommand;
import com.takodu.api.conversations.domain.model.commands.MarkConversationReadCommand;
import com.takodu.api.conversations.domain.model.commands.PauseAssistantCommand;
import com.takodu.api.conversations.domain.model.commands.ResumeAssistantCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/** W8, W9, A3: inbox housekeeping, allowed while the plan is inactive because it sends nothing. */
public interface ConversationCommandService {

    /**
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     */
    Conversation handle(MarkConversationReadCommand command);

    /**
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     * @throws MemberNotFoundException when the member is not an active member of the organization
     * @throws MemberNotAtEstablishmentException when the member cannot see that inbox
     */
    Conversation handle(AssignConversationCommand command);

    /**
     * A3: keeps the assistant quiet on the chat until someone resumes it. A repeated key returns the chat unchanged.
     *
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     */
    Conversation handle(PauseAssistantCommand command);

    /**
     * A3, A7: clears the 12-hour pause, a manual pause and a handoff. A repeated key returns the chat unchanged.
     *
     * @throws ConversationNotFoundException when the conversation is not in the organization
     * @throws AccessDeniedException when staff are not assigned to its establishment
     */
    Conversation handle(ResumeAssistantCommand command);

    /**
     * A7: the assistant hands the chat to a person. Runs without a caller, for the assistant's turn.
     *
     * @throws ConversationNotFoundException when the conversation is not in the organization
     */
    Conversation handle(HandOffConversationCommand command);

    /**
     * Tells the open inboxes that the assistant started or finished answering the chat; changes nothing. Runs without
     * a caller or a transaction, for the assistant's turn. A conversation outside the organization is ignored.
     */
    void handle(ReportAssistantActivityCommand command);
}
