package com.takodu.api.iam.infrastructure.persistence.jpa.adapters;

import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.repositories.UserRepository;
import com.takodu.api.iam.infrastructure.persistence.jpa.assemblers.UserPersistenceAssembler;
import com.takodu.api.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository {
    private static final String EMAIL_UNIQUE = "users_email_uk";

    private final UserPersistenceRepository userPersistenceRepository;

    public UserRepositoryImpl(UserPersistenceRepository userPersistenceRepository) {
        this.userPersistenceRepository = userPersistenceRepository;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userPersistenceRepository.findByEmail(email).map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userPersistenceRepository.findByIdOptional(userId.value()).map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public User save(User user) {
        var entity = UserPersistenceAssembler.toPersistenceFromDomain(user);
        try {
            if (entity.getId() == null) {
                userPersistenceRepository.persist(entity);
            } else {
                entity = userPersistenceRepository.getEntityManager().merge(entity);
            }
            userPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case EMAIL_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return UserPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
