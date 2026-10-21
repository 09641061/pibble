package com.takodu.api.customers.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Active customers whose name, WhatsApp name or phone contains the text.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param text blank lists every active customer
 * @param page zero-based
 */
public record SearchCustomersQuery(OrganizationId organizationId, String text, int page, int size) {
    public static final int MAX_SIZE = 100;

    public SearchCustomersQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (page < 0) throw new IllegalArgumentException("page must be zero or more");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size must be between 1 and 100");
        text = text == null ? "" : text.strip();
    }
}
