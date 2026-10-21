package com.takodu.api.iam.application.acl;

import com.takodu.api.iam.application.queryservices.UserQueryService;
import com.takodu.api.iam.domain.model.queries.GetUserByIdQuery;
import com.takodu.api.iam.interfaces.acl.IamContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IamContextFacadeImpl implements IamContextFacade {
    private final UserQueryService userQueryService;

    public IamContextFacadeImpl(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<String> fetchUserEmail(UUID userId) {
        return userQueryService.handle(new GetUserByIdQuery(new UserId(userId))).map(user -> user.getEmail().value());
    }
}
