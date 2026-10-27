package com.takodu.api.assistant.application.internal.outboundservices.acl;

import com.takodu.api.assistant.domain.model.valueobjects.AssistantReply;
import com.takodu.api.assistant.domain.model.valueobjects.ChatLine;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationState;
import com.takodu.api.conversations.interfaces.acl.ConversationsContextFacade;
import com.takodu.api.conversations.interfaces.acl.ReplyChoicesView;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** The only class in assistant that talks to conversations. */
@ApplicationScoped
public class ExternalConversationService {
    private final ConversationsContextFacade conversationsContextFacade;

    public ExternalConversationService(ConversationsContextFacade conversationsContextFacade) {
        this.conversationsContextFacade = conversationsContextFacade;
    }

    /** @return the chat's state for A2, empty when it is not in the organization */
    public Optional<ConversationState> fetchConversation(OrganizationId organizationId, ConversationId conversationId) {
        return conversationsContextFacade.fetchConversation(organizationId.value(), conversationId.value())
                .map(view -> new ConversationState(new ConversationId(view.conversationId()),
                        new EstablishmentId(view.establishmentId()), new CustomerId(view.customerId()),
                        view.lineConnected(), view.windowOpen(), view.assistantPaused()));
    }

    /** @return the last messages of the chat, oldest first */
    public List<ChatLine> fetchRecentMessages(OrganizationId organizationId, ConversationId conversationId,
                                              int limit) {
        return conversationsContextFacade.fetchRecentMessages(organizationId.value(), conversationId.value(), limit)
                .stream()
                .map(line -> new ChatLine(ChatLine.ChatRole.valueOf(line.role().toUpperCase(Locale.ROOT)),
                        line.text(), line.at()))
                .toList();
    }

    /**
     * Sends the reply in the format its options fit (08 §5).
     *
     * @return whether the reply is queued; false when the assistant may no longer answer the chat (A2)
     */
    public boolean sendReply(OrganizationId organizationId, ConversationId conversationId, AssistantReply reply,
                             UUID idempotencyKey) {
        var choices = switch (reply.format()) {
            case TEXT -> null;
            case BUTTONS -> new ReplyChoicesView(ReplyChoicesView.Style.BUTTONS, reply.options());
            case LIST -> new ReplyChoicesView(ReplyChoicesView.Style.LIST, reply.options());
        };
        return conversationsContextFacade.sendAssistantReply(organizationId.value(), conversationId.value(),
                reply.body(), choices, idempotencyKey);
    }

    /** A7 */
    public void handOff(OrganizationId organizationId, ConversationId conversationId, String summary) {
        conversationsContextFacade.handOff(organizationId.value(), conversationId.value(), summary);
    }

    /** The inbox shows the assistant typing while a turn runs. */
    public void reportActivity(OrganizationId organizationId, ConversationId conversationId, boolean typing) {
        conversationsContextFacade.reportAssistantActivity(organizationId.value(), conversationId.value(), typing);
    }
}
