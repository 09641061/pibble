package com.takodu.api.shared.application.tenancy;

import com.takodu.api.shared.domain.model.valueobjects.Role;

import java.util.Set;

/**
 * Fixed permissions per role (product docs §5). Changing a row is a product decision. The assistant is no role: it may
 * only book, reschedule and cancel for the customer it is talking to (A5).
 */
public enum Action {
    MANAGE_ORGANIZATION(Role.OWNER),
    MANAGE_ESTABLISHMENTS(Role.OWNER),
    CONNECT_WHATSAPP(Role.OWNER),
    MANAGE_SERVICES_AND_HOURS(Role.OWNER, Role.MANAGER),
    MANAGE_MEMBERS(Role.OWNER, Role.MANAGER),
    MANAGE_ANY_CALENDAR(Role.OWNER, Role.MANAGER),
    MANAGE_OWN_TIME_BLOCKS(Role.OWNER, Role.MANAGER, Role.STAFF),
    BOOK_APPOINTMENTS(Role.OWNER, Role.MANAGER, Role.STAFF),
    REPLY_IN_INBOX(Role.OWNER, Role.MANAGER, Role.STAFF),
    EDIT_CUSTOMERS(Role.OWNER, Role.MANAGER, Role.STAFF),
    DELETE_CUSTOMERS(Role.OWNER, Role.MANAGER);

    private final Set<Role> allowed;

    Action(Role... allowed) {
        this.allowed = Set.of(allowed);
    }

    /**
     * @param role the caller's role
     * @return whether the role may perform the action
     */
    public boolean allows(Role role) {
        return allowed.contains(role);
    }

    /** @return whether the assistant may perform the action, in its own establishment (A5) */
    public boolean allowsAssistant() {
        return this == BOOK_APPOINTMENTS;
    }
}
