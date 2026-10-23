package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * 07 §7: staff send the reminder or the confirmation of one of the customer's upcoming appointments.
 *
 * @param sentBy the caller's membership, from {@code TenantContext}
 * @param kind {@code REMINDER} or {@code CONFIRMATION}; a cancellation goes only when staff cancel
 */
public record SendStaffTemplateCommand(OrganizationId organizationId, ConversationId conversationId, MemberId sentBy,
                                       NotificationKind kind, AppointmentId appointmentId,
                                       IdempotencyKey idempotencyKey) {
    public SendStaffTemplateCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (sentBy == null) throw new IllegalArgumentException("sentBy is required");
        if (kind == null || kind == NotificationKind.CANCELLATION)
            throw new IllegalArgumentException("template must be reminder or confirmation");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
