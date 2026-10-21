package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.Session;

public interface SessionRepository {
    Session save(Session session);
}
