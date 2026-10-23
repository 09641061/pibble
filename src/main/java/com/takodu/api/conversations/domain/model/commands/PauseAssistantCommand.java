package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param pausedBy the member who pressed "pause assistant", from the tenant context */
public record PauseAssistantCommand(OrganizationId organizationId, ConversationId conversationId, MemberId pausedBy,
                                    IdempotencyKey idempotencyKey) {
    public PauseAssistantCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (pausedBy == null) throw new IllegalArgumentException("pausedBy is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
