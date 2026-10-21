package com.takodu.api.iam.application.queryservices;

import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {
    /** @return the user, empty when no user has this id */
    Optional<User> handle(GetUserByIdQuery query);
}
