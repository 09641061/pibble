package com.takodu.api.iam.infrastructure.persistence.jpa.adapters;

import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.repositories.LoginCodeRepository;
import com.takodu.api.iam.infrastructure.persistence.jpa.assemblers.LoginCodePersistenceAssembler;
import com.takodu.api.iam.infrastructure.persistence.jpa.repositories.LoginCodePersistenceRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class LoginCodeRepositoryImpl implements LoginCodeRepository {
    private final LoginCodePersistenceRepository loginCodePersistenceRepository;

    public LoginCodeRepositoryImpl(LoginCodePersistenceRepository loginCodePersistenceRepository) {
        this.loginCodePersistenceRepository = loginCodePersistenceRepository;
    }

    /** Counts by expiry, which is the issue instant plus {@link LoginCode#TTL}, so the domain's clock decides. */
    @Override
    public long countIssuedSince(Email email, Instant since) {
        return loginCodePersistenceRepository.countExpiringAfter(email, since.plus(LoginCode.TTL));
    }

    @Override
    public List<LoginCode> findUsableForUpdate(Email email, Instant now) {
        return loginCodePersistenceRepository.findUsableForUpdate(email, now, LoginCode.MAX_ATTEMPTS).stream()
                .map(LoginCodePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public LoginCode save(LoginCode loginCode) {
        var entity = LoginCodePersistenceAssembler.toPersistenceFromDomain(loginCode);
        if (entity.getId() == null) {
            loginCodePersistenceRepository.persist(entity);
        } else {
            entity = loginCodePersistenceRepository.getEntityManager().merge(entity);
        }
        loginCodePersistenceRepository.flush();
        return LoginCodePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
