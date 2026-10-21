package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId userId);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent sign-in
     *         created the same email first
     */
    User save(User user);
}
