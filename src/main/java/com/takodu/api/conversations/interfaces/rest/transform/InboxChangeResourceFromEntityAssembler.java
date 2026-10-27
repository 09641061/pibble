package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.events.AppointmentChangedEvent;
import com.takodu.api.conversations.domain.model.events.AssistantActivityEvent;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.InboxEvent;
import com.takodu.api.conversations.interfaces.rest.resources.InboxChangeResource;
import com.takodu.api.conversations.interfaces.rest.resources.InboxConversationSummaryResource;

import java.util.Locale;

public final class InboxChangeResourceFromEntityAssembler {
    private InboxChangeResourceFromEntityAssembler() { }

    public static InboxChangeResource toResourceFromEntity(InboxEvent event) {
        var establishmentId = event.establishmentId().value();
        return switch (event) {
            case ConversationChangedEvent changed -> new InboxChangeResource("conversation",
                    changed.conversationId().value(), establishmentId, new InboxConversationSummaryResource(
                    changed.lastMessageAt(), changed.lastInboundAt(), changed.unreadCount(), changed.windowClosesAt(),
                    changed.assistantPausedUntil(), changed.assistantHandedOff(), changed.assistantPausedManually()),
                    null);
            case AssistantActivityEvent activity -> new InboxChangeResource(
                    activity.typing() ? "assistant_typing" : "assistant_idle", activity.conversationId().value(),
                    establishmentId, null, null);
            case AppointmentChangedEvent appointment -> new InboxChangeResource(
                    "appointment_" + appointment.change().name().toLowerCase(Locale.ROOT), null, establishmentId,
                    null, appointment.appointmentId().value());
        };
    }
}
