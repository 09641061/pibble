package com.takodu.api.iam.interfaces.rest.transform;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.iam.interfaces.rest.resources.SessionResource;

public final class SessionResourceFromEntityAssembler {
    private SessionResourceFromEntityAssembler() { }

    public static SessionResource toResourceFromEntity(Session session, SessionId current) {
        return new SessionResource(session.getId().value(), session.getLastUsedAt(), session.getExpiresAt(),
                session.getId().equals(current));
    }
}
