package com.takodu.api.iam.application.internal.queryservices;

import com.takodu.api.iam.application.queryservices.SessionQueryService;
import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.queries.GetActiveSessionsByUserQuery;
import com.takodu.api.iam.domain.repositories.SessionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.List;

@ApplicationScoped
public class SessionQueryServiceImpl implements SessionQueryService {
    private final SessionRepository sessionRepository;
    private final Clock clock;

    public SessionQueryServiceImpl(SessionRepository sessionRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Override
    public List<Session> handle(GetActiveSessionsByUserQuery query) {
        return sessionRepository.findActiveByUser(query.userId(), clock.instant());
    }
}
