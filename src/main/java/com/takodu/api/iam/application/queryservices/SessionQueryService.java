package com.takodu.api.iam.application.queryservices;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.queries.GetActiveSessionsByUserQuery;

import java.util.List;

public interface SessionQueryService {
    /** @return the user's unrevoked, unexpired sessions, most recently used first */
    List<Session> handle(GetActiveSessionsByUserQuery query);
}
