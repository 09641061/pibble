package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.MemberNotAtEstablishmentException;
import com.takodu.api.conversations.domain.exceptions.MemberNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.commands.AssignConversationCommand;
import com.takodu.api.conversations.domain.model.commands.HandOffConversationCommand;
import com.takodu.api.conversations.domain.model.commands.MarkConversationReadCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/** W8, W9: inbox housekeeping, allowed while the plan is inactive because it sends nothing. */
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
     * A7: the assistant hands the chat to a person. Runs without a caller, for the assistant's turn.
     *
     * @throws ConversationNotFoundException when the conversation is not in the organization
     */
    Conversation handle(HandOffConversationCommand command);
}
