package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.PlanLimitReachedException;
import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * The business as a customer of Takodu: it holds the plan, the team and the customer list.
 */
@Getter
public class Organization {
    private final OrganizationId id;
    private String name;
    private final Plan plan;
    private final Instant planActiveUntil;
    private final IdempotencyKey idempotencyKey;
    private IdempotencyKey lastIdempotencyKey;

    /**
     * O1, O5: a new organization starts on {@code standard} with a trial; the Takodu team sets the plan after
     * payment.
     *
     * @param trialUntil end of the trial period
     * @param idempotencyKey the creating request's key
     */
    public static Organization create(String name, Instant trialUntil, IdempotencyKey idempotencyKey) {
        return new Organization(null, name, Plan.STANDARD, Objects.requireNonNull(trialUntil, "trialUntil is required"),
                Objects.requireNonNull(idempotencyKey, "idempotencyKey is required"), null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Organization(OrganizationId id, String name, Plan plan, Instant planActiveUntil,
                        IdempotencyKey idempotencyKey, IdempotencyKey lastIdempotencyKey) {
        this.id = id;
        this.name = requireName(name);
        this.plan = Objects.requireNonNull(plan, "plan is required");
        this.planActiveUntil = planActiveUntil;
        this.idempotencyKey = idempotencyKey;
        this.lastIdempotencyKey = lastIdempotencyKey;
    }

    /** O2 */
    public void rename(String name) {
        this.name = requireName(name);
    }

    /** O5: the plan is active while {@code plan_active_until} is in the future. */
    public boolean isPlanActive(Instant now) {
        return planActiveUntil != null && now.isBefore(planActiveUntil);
    }

    /**
     * O6: settings cannot change while the plan is inactive.
     *
     * @throws PlanInactiveException when the plan is not active at {@code now}
     */
    public void requireActivePlan(Instant now) {
        if (!isPlanActive(now)) throw new PlanInactiveException(id.value());
    }

    /**
     * O4: the caller passes the count read under a lock on this organization.
     *
     * @throws PlanLimitReachedException when the plan allows no more active establishments
     */
    public void ensureCanAddEstablishment(int activeEstablishments) {
        if (!plan.allows(activeEstablishments)) throw new PlanLimitReachedException(plan);
    }

    /** Records the key of an ownership transfer, so a retry finds it already applied (O12). */
    public void recordTransfer(IdempotencyKey idempotencyKey) {
        this.lastIdempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    }

    /** @return whether the transfer with this key was already applied */
    public boolean alreadyApplied(IdempotencyKey idempotencyKey) {
        return idempotencyKey.equals(lastIdempotencyKey);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Organization name must not be blank");
        return name.strip();
    }
}
