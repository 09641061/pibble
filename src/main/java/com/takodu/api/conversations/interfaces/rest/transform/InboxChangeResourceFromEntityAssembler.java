package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.events.AssistantActivityEvent;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.InboxEvent;
import com.takodu.api.conversations.interfaces.rest.resources.InboxChangeResource;
import com.takodu.api.conversations.interfaces.rest.resources.InboxConversationSummaryResource;

public final class InboxChangeResourceFromEntityAssembler {
    private InboxChangeResourceFromEntityAssembler() { }

    public static InboxChangeResource toResourceFromEntity(InboxEvent event) {
        var conversationId = event.conversationId().value();
        var establishmentId = event.establishmentId().value();
        return switch (event) {
            case ConversationChangedEvent changed -> new InboxChangeResource("conversation", conversationId,
                    establishmentId, new InboxConversationSummaryResource(changed.lastMessageAt(),
                    changed.lastInboundAt(), changed.unreadCount(), changed.windowClosesAt(),
                    changed.assistantPausedUntil(), changed.assistantHandedOff(), changed.assistantPausedManually()));
            case AssistantActivityEvent activity -> new InboxChangeResource(
                    activity.typing() ? "assistant_typing" : "assistant_idle", conversationId, establishmentId, null);
        };
    }
}
