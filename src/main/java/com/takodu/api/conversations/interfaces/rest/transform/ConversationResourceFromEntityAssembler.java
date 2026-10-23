package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.application.queryservices.InboxConversation;
import com.takodu.api.conversations.interfaces.rest.resources.ConversationPageResource;
import com.takodu.api.conversations.interfaces.rest.resources.ConversationResource;

import java.util.List;

public final class ConversationResourceFromEntityAssembler {
    private ConversationResourceFromEntityAssembler() { }

    public static ConversationResource toResourceFromEntity(InboxConversation entry) {
        var conversation = entry.conversation();
        var customer = entry.customer();
        return new ConversationResource(conversation.getId().value(), conversation.getEstablishmentId().value(),
                conversation.getCustomerId().value(), customer == null ? null : customer.displayName(),
                customer == null ? null : customer.phone(), customer == null || customer.deleted(),
                conversation.getAssignedMemberId() == null ? null : conversation.getAssignedMemberId().value(),
                conversation.getUnreadCount(), conversation.getLastMessageAt(), conversation.getLastInboundAt(),
                entry.windowOpen(), conversation.windowClosesAt(), entry.assistantPaused(),
                conversation.getAssistantPausedUntil(), conversation.isHandedOff(), conversation.getHandoffSummary(),
                conversation.isManuallyPaused(), conversation.getManuallyPausedBy() == null ? null
                        : conversation.getManuallyPausedBy().value());
    }

    /** @param size the page size asked for; a full page may have more after it */
    public static ConversationPageResource toPageFromEntities(List<InboxConversation> entries, int size) {
        var last = entries.isEmpty() ? null : entries.getLast().conversation();
        return new ConversationPageResource(
                entries.stream().map(ConversationResourceFromEntityAssembler::toResourceFromEntity).toList(),
                entries.size() < size ? null : PageCursorAssembler.toTokenFromCursor(last.getLastMessageAt(),
                        last.getId().value()));
    }
}
