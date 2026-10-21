package com.takodu.api.iam.application.internal.queryservices;

import com.takodu.api.iam.application.queryservices.UserQueryService;
import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.domain.model.queries.GetUserByIdQuery;
import com.takodu.api.iam.domain.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }
}
