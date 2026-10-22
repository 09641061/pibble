package com.takodu.api.assistant.application.internal.tools;

import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.CustomerAppointment;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.RequestScoped;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A4: the turn the tools act for, bound by {@code TurnWorker} before the model runs. The model never chooses an
 * organization, establishment, customer or appointment; the tools read them from here. One per request context,
 * and the worker opens one per turn.
 */
@RequestScoped
public class TurnScope {
    /** 08 §7: a turn runs at most this many tools. */
    public static final int MAX_TOOL_CALLS = 8;

    private AssistantTurn turn;
    private EstablishmentProfile establishment;
    private String businessInfo;
    private List<CustomerAppointment> listedAppointments;
    private int toolCalls;
    private String handOffSummary;

    public void bind(AssistantTurn turn, EstablishmentProfile establishment, String businessInfo) {
        if (this.turn != null) throw new IllegalStateException("TurnScope is already bound");
        this.turn = Objects.requireNonNull(turn, "turn is required");
        this.establishment = Objects.requireNonNull(establishment, "establishment is required");
        this.businessInfo = Objects.requireNonNull(businessInfo, "businessInfo is required");
    }

    public AssistantTurnId turnId() {
        return bound().getId();
    }

    public OrganizationId organizationId() {
        return bound().getOrganizationId();
    }

    public EstablishmentId establishmentId() {
        return bound().getEstablishmentId();
    }

    public ConversationId conversationId() {
        return bound().getConversationId();
    }

    public CustomerId customerId() {
        return bound().getCustomerId();
    }

    public EstablishmentProfile establishment() {
        bound();
        return establishment;
    }

    public ZoneId zone() {
        return establishment().zone();
    }

    public String businessInfo() {
        bound();
        return businessInfo;
    }

    /** @return whether the tool may run: false once the turn used its {@link #MAX_TOOL_CALLS} */
    public boolean countToolCall() {
        bound();
        return ++toolCalls <= MAX_TOOL_CALLS;
    }

    public int toolCalls() {
        return Math.min(toolCalls, MAX_TOOL_CALLS);
    }

    /** The appointments as the customer last saw them numbered, so a number from the model finds the same one. */
    public void rememberAppointments(List<CustomerAppointment> appointments) {
        bound();
        listedAppointments = List.copyOf(appointments);
    }

    /** @return the last numbered list, empty when none was shown in this turn */
    public Optional<List<CustomerAppointment>> listedAppointments() {
        bound();
        return Optional.ofNullable(listedAppointments);
    }

    /** A7: a person takes over after this turn's reply; the first reason wins. */
    public void requestHandOff(String summary) {
        bound();
        if (handOffSummary == null) handOffSummary = Objects.requireNonNull(summary, "summary is required");
    }

    public Optional<String> handOffSummary() {
        return Optional.ofNullable(handOffSummary);
    }

    private AssistantTurn bound() {
        if (turn == null) throw new IllegalStateException("No turn is bound: the tools run only inside a turn");
        return turn;
    }
}
