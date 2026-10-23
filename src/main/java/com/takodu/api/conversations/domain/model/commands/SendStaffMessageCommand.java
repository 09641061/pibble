package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param sentBy the caller's membership, from {@code TenantContext} */
public record SendStaffMessageCommand(OrganizationId organizationId, ConversationId conversationId, MemberId sentBy,
                                      String body, IdempotencyKey idempotencyKey) {
    public SendStaffMessageCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (sentBy == null) throw new IllegalArgumentException("sentBy is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
