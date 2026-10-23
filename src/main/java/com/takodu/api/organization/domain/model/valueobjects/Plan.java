package com.takodu.api.organization.domain.model.valueobjects;

/** O4: the only place where plans and their establishment limits are defined. Prices live outside the system. */
public enum Plan {
    STANDARD(1), MAX(Integer.MAX_VALUE);

    private final int establishmentLimit;

    Plan(int establishmentLimit) {
        this.establishmentLimit = establishmentLimit;
    }

    /**
     * @param activeEstablishments how many establishments are active now
     * @return whether one more may be opened; existing ones above the limit after a downgrade stay open
     */
    public boolean allows(int activeEstablishments) {
        return activeEstablishments < establishmentLimit;
    }
}
