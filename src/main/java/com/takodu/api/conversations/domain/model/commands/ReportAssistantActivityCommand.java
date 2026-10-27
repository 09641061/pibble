package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param typing true when the assistant started answering the conversation, false when it finished */
public record ReportAssistantActivityCommand(OrganizationId organizationId, ConversationId conversationId,
                                             boolean typing) {
    public ReportAssistantActivityCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
    }
}
