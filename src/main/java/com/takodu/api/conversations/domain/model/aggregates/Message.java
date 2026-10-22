package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageDirection;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import com.takodu.api.conversations.domain.model.valueobjects.MessageOrigin;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final List<String> templateParameters;
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
                List.of(), DeliveryStatus.RECEIVED, null, null, null, null, 0, null, null, at);
    }

    /** WhatsApp's limit for a text message. */
    public static final int MAX_TEXT = 4096;
    /** The first retry waits this long; each next one waits twice as long (30 s, 1 min, 2 min, 4 min). */
    public static final Duration FIRST_RETRY = Duration.ofSeconds(30);

    /**
     * W6, W7: a staff reply, queued as the outbox row the sender job delivers.
     *
     * @throws IllegalArgumentException when the text is blank or longer than 4,096 characters
     */
    public static Message staffText(Conversation conversation, MemberId sentBy, String body, Instant now,
                                    IdempotencyKey idempotencyKey) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Write a message");
        if (body.length() > MAX_TEXT) throw new IllegalArgumentException("A message has at most 4096 characters");
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.OUT,
                MessageOrigin.STAFF, null, MessageContent.text(body), null, List.of(), DeliveryStatus.PENDING, null,
                null, null, Objects.requireNonNull(sentBy, "Staff messages record who sent them"), 0, now, idempotencyKey, now);
    }

    /**
     * A8: the assistant's reply, queued for the sender job like a staff reply.
     *
     * @param idempotencyKey derived from the turn, so a retried turn queues it once
     * @throws IllegalArgumentException when the text is blank or longer than 4,096 characters
     */
    public static Message assistantText(Conversation conversation, String body, Instant now,
                                        IdempotencyKey idempotencyKey) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("The assistant's reply is empty");
        if (body.length() > MAX_TEXT) throw new IllegalArgumentException("A message has at most 4096 characters");
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.OUT,
                MessageOrigin.ASSISTANT, null, MessageContent.text(body), null, List.of(), DeliveryStatus.PENDING, null,
                null, null, null, 0, now, Objects.requireNonNull(idempotencyKey, "idempotencyKey is required"), now);
    }

    /**
     * W11-W13: an appointment notification, queued for the sender job. Outside the window it goes as the approved
     * template; inside, as the same text. Either way the inbox shows the text the customer reads.
     *
     * @param asTemplate whether it must go as the template: always for reminders, and whenever the window is closed
     */
    public static Message notification(Conversation conversation, NotificationTemplate template,
                                       List<String> parameters, boolean asTemplate, Instant now) {
        var body = template.render(parameters);
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.OUT,
                MessageOrigin.SYSTEM, null, new MessageContent(asTemplate ? MessageKind.TEMPLATE : MessageKind.TEXT,
                body, null, null), asTemplate ? template.metaName() : null,
                asTemplate ? template.parameters(parameters) : List.of(), DeliveryStatus.PENDING, null, null, null,
                null, 0, now, null, now);
    }

    /** W5: the owner replied from the WhatsApp Business app; WhatsApp already sent it. */
    public static Message businessAppEcho(Conversation conversation, Wamid wamid, MessageContent content, Instant at) {
        return new Message(null, conversation.getOrganizationId(), conversation.getId(), MessageDirection.OUT,
                MessageOrigin.BUSINESS_APP, Objects.requireNonNull(wamid, "wamid is required"), content, null,
                List.of(), DeliveryStatus.SENT, null, null, null, null, 0, null, null, at);
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
                Objects.requireNonNull(wamid, "wamid is required"), content, null, List.of(),
                fromCustomer ? DeliveryStatus.RECEIVED : status, null, null, null, null, 0, null, null, at);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Message(MessageId id, OrganizationId organizationId, ConversationId conversationId,
                   MessageDirection direction, MessageOrigin origin, Wamid wamid, MessageContent content,
                   String templateName, List<String> templateParameters, DeliveryStatus status, String error,
                   String pricingCategory, Boolean billable, MemberId sentBy, int attempts, Instant nextAttemptAt,
                   IdempotencyKey idempotencyKey, Instant occurredAt) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId is required");
        this.direction = Objects.requireNonNull(direction, "direction is required");
        this.origin = Objects.requireNonNull(origin, "origin is required");
        this.wamid = wamid;
        this.content = Objects.requireNonNull(content, "content is required");
        this.templateName = templateName;
        this.templateParameters = List.copyOf(templateParameters);
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

    /** Meta accepted the message. */
    public void markSent(Wamid wamid) {
        this.wamid = Objects.requireNonNull(wamid, "wamid is required");
        this.status = status.advanceTo(DeliveryStatus.SENT);
        this.nextAttemptAt = null;
        this.attempts++;
    }

    /**
     * W14: a send that failed is retried later, with backoff, until the fifth attempt; then the message is shown
     * failed in the inbox. A failure Meta will repeat, such as a closed window, fails at once.
     *
     * @param retryable whether trying again may succeed, as after a timeout or Meta's rate limit
     */
    public void recordSendFailure(String error, boolean retryable, Instant now) {
        attempts++;
        this.error = error;
        if (!retryable || attempts >= MAX_ATTEMPTS) {
            fail(error);
            return;
        }
        nextAttemptAt = now.plus(FIRST_RETRY.multipliedBy(1L << (attempts - 1)));
    }

    /** The rule that allowed the send no longer holds (line disconnected, plan inactive, window closed). */
    public void fail(String reason) {
        status = status.advanceTo(DeliveryStatus.FAILED);
        error = reason;
        nextAttemptAt = null;
    }

    /** @return whether the outbox still has to send it */
    public boolean isPending() {
        return status == DeliveryStatus.PENDING;
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
