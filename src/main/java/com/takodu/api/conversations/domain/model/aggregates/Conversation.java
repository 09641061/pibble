package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * W3: the chat between one line and one customer. It keeps only the summary its rules need (the window, the unread
 * count, the assignee and the assistant pause); messages are their own aggregate, so a chat is never loaded whole.
 */
@Getter
public class Conversation {
    /** W6: free-form messages may be sent until 24 hours after the customer's last message. */
    public static final Duration SERVICE_WINDOW = Duration.ofHours(24);
    /** A3: the assistant stays quiet for 12 hours after a person writes, sliding with each human message. */
    public static final Duration HUMAN_PAUSE = Duration.ofHours(12);
    /** What the inbox shows of a handoff's reason. */
    public static final int MAX_HANDOFF_SUMMARY = 500;

    private final ConversationId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private final WhatsappLineId lineId;
    private final CustomerId customerId;
    private MemberId assignedMemberId;
    private Instant assistantPausedUntil;
    private Instant lastInboundAt;
    private Instant lastMessageAt;
    private int unreadCount;
    private Instant assistantHandedOffAt;
    private String handoffSummary;

    /** @param firstMessageAt when the message that starts the conversation was sent */
    public static Conversation start(WhatsappLine line, CustomerId customerId, Instant firstMessageAt) {
        return new Conversation(null, line.getOrganizationId(), line.getEstablishmentId(), line.getId(), customerId,
                null, null, null, firstMessageAt, 0, null, null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Conversation(ConversationId id, OrganizationId organizationId, EstablishmentId establishmentId,
                        WhatsappLineId lineId, CustomerId customerId, MemberId assignedMemberId,
                        Instant assistantPausedUntil, Instant lastInboundAt, Instant lastMessageAt, int unreadCount,
                        Instant assistantHandedOffAt, String handoffSummary) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.lineId = Objects.requireNonNull(lineId, "lineId is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.assignedMemberId = assignedMemberId;
        this.assistantPausedUntil = assistantPausedUntil;
        this.lastInboundAt = lastInboundAt;
        this.lastMessageAt = Objects.requireNonNull(lastMessageAt, "lastMessageAt is required");
        this.unreadCount = unreadCount;
        this.assistantHandedOffAt = assistantHandedOffAt;
        this.handoffSummary = handoffSummary;
    }

    /** W6, W8: the customer wrote; the window reopens and staff have one more message to read. */
    public void recordInbound(Instant at) {
        if (lastInboundAt == null || at.isAfter(lastInboundAt)) lastInboundAt = at;
        touch(at);
        unreadCount++;
    }

    /**
     * A3, W5: a staff reply from Takodu or the owner's reply from the phone. A person is handling the chat, so the
     * assistant steps back and nothing is left unread.
     */
    public void recordHumanOutbound(Instant at) {
        touch(at);
        var until = at.plus(HUMAN_PAUSE);
        if (assistantPausedUntil == null || until.isAfter(assistantPausedUntil)) assistantPausedUntil = until;
        unreadCount = 0;
    }

    /** A message imported from the phone's history: it orders the inbox but opens no window and counts as read. */
    public void recordHistory(Instant at) {
        touch(at);
    }

    /** W11-W13: an appointment notification went to the outbox; it orders the inbox and pauses nobody. */
    public void recordNotification(Instant at) {
        touch(at);
    }

    /** The assistant replied; it orders the inbox and leaves the customer's messages unread for staff. */
    public void recordAssistantOutbound(Instant at) {
        touch(at);
    }

    /**
     * A7: the assistant hands the chat to a person. It stays quiet until staff resume it, and the chat shows unread
     * with the reason, so someone looks.
     *
     * @param summary what the customer needs, in the assistant's words
     */
    public void handOff(String summary, Instant now) {
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("A handoff needs a summary");
        if (assistantHandedOffAt == null) assistantHandedOffAt = now;
        handoffSummary = summary.strip().length() > MAX_HANDOFF_SUMMARY
                ? summary.strip().substring(0, MAX_HANDOFF_SUMMARY) : summary.strip();
        unreadCount = Math.max(unreadCount, 1);
    }

    /** @return whether the assistant handed the chat to a person who has not resumed it yet */
    public boolean isHandedOff() {
        return assistantHandedOffAt != null;
    }

    /** W6: the single source of truth for whether free-form messages may be sent. */
    public boolean isWindowOpen(Instant now) {
        return lastInboundAt != null && now.isBefore(windowClosesAt());
    }

    /** @return when the window closes, null when the customer never wrote */
    public Instant windowClosesAt() {
        return lastInboundAt == null ? null : lastInboundAt.plus(SERVICE_WINDOW);
    }

    /** A3, A7: a person wrote less than 12 hours ago, or the assistant handed the chat off. */
    public boolean isAssistantPaused(Instant now) {
        return isHandedOff() || assistantPausedUntil != null && now.isBefore(assistantPausedUntil);
    }

    /** W8 */
    public void markRead() {
        unreadCount = 0;
    }

    /**
     * W9: informative only; it locks nothing.
     *
     * @param memberId null to unassign
     */
    public void assignTo(MemberId memberId) {
        this.assignedMemberId = memberId;
    }

    private void touch(Instant at) {
        if (at.isAfter(lastMessageAt)) lastMessageAt = at;
    }
}
