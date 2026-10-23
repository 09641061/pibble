package com.takodu.api.iam.application.internal.commandservices;

import com.takodu.api.iam.application.commandservices.SessionCommandService;
import com.takodu.api.iam.application.commandservices.SignedInSession;
import com.takodu.api.iam.application.internal.outboundservices.tokens.AccessTokenIssuer;
import com.takodu.api.iam.domain.exceptions.InvalidLoginCodeException;
import com.takodu.api.iam.domain.exceptions.InvalidRefreshTokenException;
import com.takodu.api.iam.domain.exceptions.SessionNotFoundException;
import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.domain.model.commands.RefreshSessionCommand;
import com.takodu.api.iam.domain.model.commands.RevokeSessionCommand;
import com.takodu.api.iam.domain.model.commands.SignInCommand;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.iam.domain.repositories.LoginCodeRepository;
import com.takodu.api.iam.domain.repositories.SessionRepository;
import com.takodu.api.iam.domain.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class SessionCommandServiceImpl implements SessionCommandService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginCodeRepository loginCodeRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public SessionCommandServiceImpl(LoginCodeRepository loginCodeRepository, UserRepository userRepository,
                                     SessionRepository sessionRepository, AccessTokenIssuer accessTokenIssuer,
                                     Clock clock) {
        this.loginCodeRepository = loginCodeRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
    }

    /** Commits on a wrong code, so the attempt counts although the request fails. */
    @Override
    @Transactional(dontRollbackOn = InvalidLoginCodeException.class)
    public SignedInSession handle(SignInCommand command) {
        var now = clock.instant();
        var presented = SecretHash.of(command.code());
        var codes = loginCodeRepository.findUsableForUpdate(command.email(), now).stream()
                .filter(code -> code.isUsable(now))
                .toList();
        if (codes.stream().noneMatch(code -> code.matches(presented))) {
            codes.forEach(code -> {
                code.recordFailedAttempt();
                loginCodeRepository.save(code);
            });
            throw new InvalidLoginCodeException();
        }
        var user = userRepository.findByEmail(command.email()).orElseGet(() -> User.register(command.email()));
        user.signIn(now);
        codes.forEach(code -> consume(code, now));
        return openSession(userRepository.save(user), now);
    }

    /** Commits on a refused token, so a late reuse stays revoked. */
    @Override
    @Transactional(dontRollbackOn = InvalidRefreshTokenException.class)
    public SignedInSession handle(RefreshSessionCommand command) {
        var now = clock.instant();
        var session = sessionRepository.findByRefreshTokenForUpdate(SecretHash.of(command.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);
        var refreshToken = newRefreshToken();
        try {
            session.refresh(SecretHash.of(command.refreshToken()), SecretHash.of(refreshToken), now);
        } catch (InvalidRefreshTokenException e) {
            sessionRepository.save(session);
            throw e;
        }
        session = sessionRepository.save(session);
        var user = userRepository.findById(session.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
        user.requireEnabled();
        return new SignedInSession(user, session, refreshToken, accessTokenIssuer.issue(user.getId(), session.getId(), now));
    }

    @Override
    @Transactional
    public Session handle(RevokeSessionCommand command) {
        var session = sessionRepository.findByIdAndUser(command.sessionId(), command.userId())
                .orElseThrow(() -> new SessionNotFoundException(command.sessionId()));
        session.revoke(clock.instant());
        return sessionRepository.save(session);
    }

    private void consume(LoginCode code, Instant now) {
        code.consume(now);
        loginCodeRepository.save(code);
    }

    private SignedInSession openSession(User user, Instant now) {
        var refreshToken = newRefreshToken();
        var session = sessionRepository.save(Session.open(user.getId(), SecretHash.of(refreshToken), now));
        return new SignedInSession(user, session, refreshToken, accessTokenIssuer.issue(user.getId(), session.getId(), now));
    }

    static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
