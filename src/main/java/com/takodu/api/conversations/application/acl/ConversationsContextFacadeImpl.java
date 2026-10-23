package com.takodu.api.conversations.application.acl;

import com.takodu.api.conversations.application.commandservices.ConversationCommandService;
import com.takodu.api.conversations.application.commandservices.MessageCommandService;
import com.takodu.api.conversations.application.queryservices.ConversationQueryService;
import com.takodu.api.conversations.application.queryservices.MessageQueryService;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.commands.HandOffConversationCommand;
import com.takodu.api.conversations.domain.model.commands.SendAssistantReplyCommand;
import com.takodu.api.conversations.domain.model.queries.GetConversationForAssistantQuery;
import com.takodu.api.conversations.domain.model.queries.GetRecentMessagesQuery;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.InteractiveChoices;
import com.takodu.api.conversations.domain.model.valueobjects.MessageDirection;
import com.takodu.api.conversations.interfaces.acl.AssistantConversationView;
import com.takodu.api.conversations.interfaces.acl.ChatLineView;
import com.takodu.api.conversations.interfaces.acl.ConversationsContextFacade;
import com.takodu.api.conversations.interfaces.acl.ReplyChoicesView;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@ApplicationScoped
public class ConversationsContextFacadeImpl implements ConversationsContextFacade {
    private final ConversationQueryService conversationQueryService;
    private final MessageQueryService messageQueryService;
    private final ConversationCommandService conversationCommandService;
    private final MessageCommandService messageCommandService;
    private final Clock clock;

    public ConversationsContextFacadeImpl(ConversationQueryService conversationQueryService,
                                          MessageQueryService messageQueryService,
                                          ConversationCommandService conversationCommandService,
                                          MessageCommandService messageCommandService, Clock clock) {
        this.conversationQueryService = conversationQueryService;
        this.messageQueryService = messageQueryService;
        this.conversationCommandService = conversationCommandService;
        this.messageCommandService = messageCommandService;
        this.clock = clock;
    }

    @Override
    public Optional<AssistantConversationView> fetchConversation(UUID organizationId, UUID conversationId) {
        var now = clock.instant();
        return conversationQueryService.handle(new GetConversationForAssistantQuery(new OrganizationId(organizationId),
                new ConversationId(conversationId))).map(found -> {
            var conversation = found.conversation();
            return new AssistantConversationView(conversation.getId().value(),
                    conversation.getEstablishmentId().value(), conversation.getCustomerId().value(),
                    found.lineConnected(), conversation.isWindowOpen(now), conversation.isAssistantPaused(now));
        });
    }

    @Override
    public List<ChatLineView> fetchRecentMessages(UUID organizationId, UUID conversationId, int limit) {
        return messageQueryService.handle(new GetRecentMessagesQuery(new OrganizationId(organizationId),
                new ConversationId(conversationId), limit)).stream().map(ConversationsContextFacadeImpl::line).toList();
    }

    @Override
    public boolean sendAssistantReply(UUID organizationId, UUID conversationId, String text, ReplyChoicesView choices,
                                      UUID idempotencyKey) {
        var interactive = choices == null ? null : new InteractiveChoices(
                InteractiveChoices.Style.valueOf(choices.style().name()), choices.titles());
        return messageCommandService.handle(new SendAssistantReplyCommand(new OrganizationId(organizationId),
                new ConversationId(conversationId), text, interactive, new IdempotencyKey(idempotencyKey))).isPresent();
    }

    @Override
    public void handOff(UUID organizationId, UUID conversationId, String summary) {
        conversationCommandService.handle(new HandOffConversationCommand(new OrganizationId(organizationId),
                new ConversationId(conversationId), summary));
    }

    /** The choices as the customer saw them, so the assistant knows what a tap answers. */
    private static String numbered(List<String> titles) {
        return String.join("\n", IntStream.range(0, titles.size())
                .mapToObj(i -> (i + 1) + ". " + titles.get(i)).toList());
    }

    private static ChatLineView line(Message message) {
        var role = switch (message.getOrigin()) {
            case CUSTOMER -> "customer";
            case ASSISTANT, SYSTEM -> "assistant";
            case STAFF, BUSINESS_APP -> "staff";
            case HISTORY -> message.getDirection() == MessageDirection.IN ? "customer" : "staff";
        };
        var content = message.getContent();
        var text = content.body() != null ? content.body()
                : "[" + content.kind().name().toLowerCase(Locale.ROOT) + "]";
        if (message.isInteractiveOut()) text = text + "\n" + numbered(message.getChoices().titles());
        return new ChatLineView(role, text, message.getOccurredAt());
    }
}
