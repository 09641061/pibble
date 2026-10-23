package com.takodu.api.assistant.domain.repositories;

import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.DailyUsage;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface AssistantTurnRepository {

    boolean existsByTriggerMessage(OrganizationId organizationId, MessageId triggerMessageId);

    /**
     * Locks the oldest turn due by {@code now} with {@code FOR UPDATE SKIP LOCKED}: a pending one whose retry time
     * came, or a running one whose lease ran out. A chat with a running turn waits, so its turns run one at a time.
     */
    @CrossTenant("The turn worker claims turns of every organization; the turn carries its organization")
    Optional<AssistantTurn> claimNext(Instant now);

    /** Locks the turn until the transaction ends. */
    Optional<AssistantTurn> lockById(OrganizationId organizationId, AssistantTurnId turnId);

    /** @return whether the customer wrote again after this turn's message and that turn waits too */
    boolean hasNewerPending(AssistantTurn turn);

    /** @return the latest customer message a finished turn of the chat answered; empty when none did */
    Optional<Instant> answeredThrough(OrganizationId organizationId, ConversationId conversationId);

    /** @return how many turns of the chat answered since the instant, for the daily limit (08 §7) */
    int countAnsweredSince(OrganizationId organizationId, ConversationId conversationId, Instant since);

    /**
     * A9: answered turns of the establishment finished in {@code [from, to)}, grouped by day in the zone.
     *
     * @return only days with turns, oldest first
     */
    List<DailyUsage> dailyUsage(OrganizationId organizationId, EstablishmentId establishmentId, ZoneId zone,
                                Instant from, Instant to);

    /** @return the establishments with answered turns finished in {@code [from, to)} */
    List<EstablishmentId> establishmentsWithUsage(OrganizationId organizationId, Instant from, Instant to);

    /**
     * @throws com.takodu.api.assistant.domain.exceptions.TurnAlreadyRunningException when another worker started a
     *         turn of the same chat meanwhile
     */
    AssistantTurn save(AssistantTurn turn);
}
