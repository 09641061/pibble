package com.takodu.api.assistant.domain.model.aggregates;

import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * One customer message the assistant must answer: the work queue row of {@code TurnWorker} and the cost record of
 * A9. A turn runs outside any transaction, because its tools write through booking in their own; while it runs, the
 * row is {@code RUNNING} and its start is a lease that another worker may take over once it is stale.
 */
@Getter
public class AssistantTurn {
    /** A7: a turn that fails this many times hands the chat to a person. */
    public static final int MAX_ATTEMPTS = 3;
    /** A failed attempt waits this long, times the attempts so far, before the next one. */
    public static final Duration RETRY_DELAY = Duration.ofSeconds(30);
    /** A running turn not finished after this long is taken as crashed and may be run again. */
    public static final Duration LEASE = Duration.ofMinutes(5);
    /** A8: the kinds the assistant reads; any other, such as audio or an image, is handed to a person (A7). */
    private static final Set<String> READABLE = Set.of("text", "interactive", "button");

    private final AssistantTurnId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private final ConversationId conversationId;
    private final CustomerId customerId;
    private final MessageId triggerMessageId;
    private final String triggerKind;
    private final Instant triggerAt;
    private TurnStatus status;
    private int attempts;
    private Instant runAfter;
    private Instant startedAt;
    private Instant answeredThrough;
    private TurnUsage usage;
    private int toolCalls;
    private String error;
    private Instant finishedAt;

    /**
     * @param triggerKind the message kind in lower case, such as {@code text} or {@code audio}
     * @param triggerAt when the customer wrote
     */
    public static AssistantTurn enqueue(OrganizationId organizationId, EstablishmentId establishmentId,
                                        ConversationId conversationId, CustomerId customerId,
                                        MessageId triggerMessageId, String triggerKind, Instant triggerAt,
                                        Instant now) {
        return new AssistantTurn(null, organizationId, establishmentId, conversationId, customerId, triggerMessageId,
                triggerKind.toLowerCase(Locale.ROOT), triggerAt, TurnStatus.PENDING, 0, now, null, null, null, 0, null,
                null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public AssistantTurn(AssistantTurnId id, OrganizationId organizationId, EstablishmentId establishmentId,
                         ConversationId conversationId, CustomerId customerId, MessageId triggerMessageId,
                         String triggerKind, Instant triggerAt, TurnStatus status, int attempts, Instant runAfter,
                         Instant startedAt, Instant answeredThrough, TurnUsage usage, int toolCalls, String error,
                         Instant finishedAt) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.triggerMessageId = Objects.requireNonNull(triggerMessageId, "triggerMessageId is required");
        this.triggerKind = Objects.requireNonNull(triggerKind, "triggerKind is required");
        this.triggerAt = Objects.requireNonNull(triggerAt, "triggerAt is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.attempts = attempts;
        this.runAfter = Objects.requireNonNull(runAfter, "runAfter is required");
        this.startedAt = startedAt;
        this.answeredThrough = answeredThrough;
        this.usage = usage;
        this.toolCalls = toolCalls;
        this.error = error;
        this.finishedAt = finishedAt;
    }

    /** @return whether the customer's message is something the assistant can read (A7, A8) */
    public boolean triggerIsReadable() {
        return READABLE.contains(triggerKind);
    }

    /**
     * Starts an attempt: a pending turn, or a running one whose worker stopped answering ({@link #isStale}).
     *
     * @throws IllegalStateException when the turn is finished or another worker holds it
     */
    public void begin(Instant now) {
        if (status != TurnStatus.PENDING && !isStale(now))
            throw new IllegalStateException("A " + status.name().toLowerCase(Locale.ROOT) + " turn cannot start");
        status = TurnStatus.RUNNING;
        attempts++;
        startedAt = now;
        error = null;
    }

    /** @return whether it is running but its worker has held it longer than {@link #LEASE} */
    public boolean isStale(Instant now) {
        return status == TurnStatus.RUNNING && !startedAt.plus(LEASE).isAfter(now);
    }

    /** A2 failed when it was run, or another turn answered its message already. Nothing was sent. */
    public void skip(String reason, Instant now) {
        requireUnfinished();
        status = TurnStatus.SKIPPED;
        error = Objects.requireNonNull(reason, "reason is required");
        finishedAt = now;
    }

    /**
     * A9: the reply went out; records what it cost.
     *
     * @param answeredThrough when the latest customer message the reply took into account was sent, so later turns
     *        for messages it already answered are skipped
     */
    public void complete(TurnUsage usage, int toolCalls, Instant answeredThrough, Instant now) {
        if (status != TurnStatus.RUNNING) throw new IllegalStateException("Only a running turn completes");
        this.usage = usage;
        this.toolCalls = toolCalls;
        this.answeredThrough = Objects.requireNonNull(answeredThrough, "answeredThrough is required");
        this.status = TurnStatus.DONE;
        this.error = null;
        this.finishedAt = now;
    }

    /**
     * A model or tool infrastructure error: tried again later, until the third attempt fails it for good (A7).
     *
     * @param usage what the failed attempt cost so far; null when unknown
     */
    public void fail(String error, TurnUsage usage, int toolCalls, Instant now) {
        if (status != TurnStatus.RUNNING) throw new IllegalStateException("Only a running turn fails");
        this.error = Objects.requireNonNull(error, "error is required");
        if (usage != null) this.usage = usage;
        this.toolCalls = toolCalls;
        if (attempts >= MAX_ATTEMPTS) {
            status = TurnStatus.FAILED;
            finishedAt = now;
        } else {
            status = TurnStatus.PENDING;
            runAfter = now.plus(RETRY_DELAY.multipliedBy(attempts));
        }
    }

    /** @return whether the last failure was the last attempt, so a person must take over (A7) */
    public boolean isFailed() {
        return status == TurnStatus.FAILED;
    }

    /** @return whether a reply sent through {@code through} covers this turn's message */
    public boolean isAnsweredBy(Instant through) {
        return through != null && !through.isBefore(triggerAt);
    }

    private void requireUnfinished() {
        if (status != TurnStatus.PENDING && status != TurnStatus.RUNNING)
            throw new IllegalStateException("The turn is already " + status.name().toLowerCase(Locale.ROOT));
    }
}
