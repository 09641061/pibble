package com.takodu.api.shared.domain.model.valueobjects;

/**
 * O9: the one role a member has in an organization. The permissions of each role are fixed in
 * {@code shared.application.tenancy.Action}.
 */
public enum Role {
    OWNER, MANAGER, STAFF;

    /**
     * @return whether the role sees every establishment of the organization; staff see only their assigned ones
     *         (O13)
     */
    public boolean seesAllEstablishments() {
        return this != STAFF;
    }
}
