package com.takodu.api.shared.application.tenancy;

import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.RequestScoped;

import java.util.Objects;

/**
 * The Takodu team member calling the internal API ({@code /internal/v1}), for one request. {@code iam}'s
 * {@code OperatorFilter} fills it; internal endpoints read it to record who acted. Operators act on any organization
 * and are not members of one, so {@link TenantContext} stays unbound on these requests.
 */
@RequestScoped
public class OperatorContext {
    private UserId userId;

    /** Called once per request by {@code OperatorFilter} after it checked the user is a platform operator. */
    public void bind(UserId userId) {
        if (this.userId != null) throw new IllegalStateException("OperatorContext is already bound");
        this.userId = Objects.requireNonNull(userId, "userId is required");
    }

    public UserId userId() {
        if (userId == null) throw new IllegalStateException("No operator for this request: only /internal/v1 binds one");
        return userId;
    }
}
