package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageDirection;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageOrigin;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * One WhatsApp message in or out. It is its own aggregate, referencing its conversation by id, so storing one never
 * loads the chat. Outbound messages start {@code PENDING} and are the outbox the sender job works through.
 */
@Getter
public class Message {
    /** W14: an outbound message is tried at most this many times, then shown failed in the inbox. */
    public static final int MAX_ATTEMPTS = 5;

    private final MessageId id;
    private final OrganizationId organizationId;
    private final ConversationId conversationId;
    private final MessageDirection direction;
    private final MessageOrigin origin;
    private Wamid wamid;
    private final MessageContent content;
    private final String templateName;
    private DeliveryStatus status;
    private String error;
    private String pricingCategory;
    private Boolean billable;
    private final MemberId sentBy;
    private int attempts;
    private Instant nextAttemptAt;
    private final IdempotencyKey idempotencyKey;
    private final Instant occurredAt;

    /** W4: a customer message from the webhook. */
    public static Message inbound(Conversation conversation, Wamid wamid, MessageContent content, Instant at) {
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.IN,
                MessageOrigin.CUSTOMER, Objects.requireNonNull(wamid, "wamid is required"), content, null,
                DeliveryStatus.RECEIVED, null, null, null, null, 0, null, null, at);
    }

    /** W5: the owner replied from the WhatsApp Business app; WhatsApp already sent it. */
    public static Message businessAppEcho(Conversation conversation, Wamid wamid, MessageContent content, Instant at) {
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.OUT,
                MessageOrigin.BUSINESS_APP, Objects.requireNonNull(wamid, "wamid is required"), content, null,
                DeliveryStatus.SENT, null, null, null, null, 0, null, null, at);
    }

    /**
     * A message of the phone's chat history, shared by the owner when the line was connected.
     *
     * @param status for a message the business sent, its last known status; ignored for the customer's messages
     */
    public static Message history(Conversation conversation, Wamid wamid, MessageContent content,
                                  boolean fromCustomer, DeliveryStatus status, Instant at) {
        return new Message(null, conversation.getOrganizationId(), conversation.getId(),
                fromCustomer ? MessageDirection.IN : MessageDirection.OUT, MessageOrigin.HISTORY,
                Objects.requireNonNull(wamid, "wamid is required"), content, null,
                fromCustomer ? DeliveryStatus.RECEIVED : status, null, null, null, null, 0, null, null, at);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Message(MessageId id, OrganizationId organizationId, ConversationId conversationId,
                   MessageDirection direction, MessageOrigin origin, Wamid wamid, MessageContent content,
                   String templateName, DeliveryStatus status, String error, String pricingCategory, Boolean billable,
                   MemberId sentBy, int attempts, Instant nextAttemptAt, IdempotencyKey idempotencyKey,
                   Instant occurredAt) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId is required");
        this.direction = Objects.requireNonNull(direction, "direction is required");
        this.origin = Objects.requireNonNull(origin, "origin is required");
        this.wamid = wamid;
        this.content = Objects.requireNonNull(content, "content is required");
        this.templateName = templateName;
        this.status = Objects.requireNonNull(status, "status is required");
        this.error = error;
        this.pricingCategory = pricingCategory;
        this.billable = billable;
        this.sentBy = sentBy;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.idempotencyKey = idempotencyKey;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    /**
     * W10: a status webhook from Meta, possibly out of order.
     *
     * @param pricingCategory Meta's pricing category, such as {@code service}; null keeps the current one
     * @param error Meta's error when {@code next} is {@code FAILED}
     */
    public void applyStatus(DeliveryStatus next, String pricingCategory, Boolean billable, String error) {
        var before = status;
        status = status.advanceTo(next);
        if (pricingCategory != null) this.pricingCategory = pricingCategory;
        if (billable != null) this.billable = billable;
        if (status == DeliveryStatus.FAILED && before != DeliveryStatus.FAILED && error != null) this.error = error;
    }
}
