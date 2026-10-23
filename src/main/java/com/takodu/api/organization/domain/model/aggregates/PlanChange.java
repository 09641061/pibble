package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.organization.domain.model.valueobjects.PlanChangeId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * O5: one change of an organization's plan by the Takodu team, kept forever so a payment dispute can be answered.
 * It has no state changes: the history is append-only, and the database rejects updates and deletes.
 */
@Getter
public class PlanChange {
    public static final int NOTE_MAX_LENGTH = 500;

    private final PlanChangeId id;
    private final OrganizationId organizationId;
    private final UserId changedBy;
    private final Plan fromPlan;
    private final Plan toPlan;
    private final Instant fromActiveUntil;
    private final Instant toActiveUntil;
    private final String note;
    private final IdempotencyKey idempotencyKey;
    private final Instant changedAt;

    /** Rehydration constructor; persistence assemblers call it, and {@link Organization#changePlan} for a new one. */
    public PlanChange(PlanChangeId id, OrganizationId organizationId, UserId changedBy, Plan fromPlan, Plan toPlan,
                      Instant fromActiveUntil, Instant toActiveUntil, String note, IdempotencyKey idempotencyKey,
                      Instant changedAt) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy is required");
        this.fromPlan = Objects.requireNonNull(fromPlan, "fromPlan is required");
        this.toPlan = Objects.requireNonNull(toPlan, "toPlan is required");
        this.fromActiveUntil = fromActiveUntil;
        this.toActiveUntil = Objects.requireNonNull(toActiveUntil, "toActiveUntil is required");
        this.note = requireNote(note);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt is required");
    }

    private static String requireNote(String note) {
        if (note == null || note.isBlank())
            throw new IllegalArgumentException("A plan change needs a note, such as how and when the business paid");
        if (note.strip().length() > NOTE_MAX_LENGTH)
            throw new IllegalArgumentException("The note must be at most " + NOTE_MAX_LENGTH + " characters");
        return note.strip();
    }
}
