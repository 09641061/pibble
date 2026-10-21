package com.takodu.api.iam.application.internal.commandservices;

import com.takodu.api.iam.application.commandservices.LoginCodeCommandService;
import com.takodu.api.iam.application.internal.outboundservices.email.EmailSender;
import com.takodu.api.iam.domain.exceptions.LoginCodeLimitReachedException;
import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.commands.RequestLoginCodeCommand;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import com.takodu.api.iam.domain.repositories.LoginCodeRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.time.Clock;

@ApplicationScoped
public class LoginCodeCommandServiceImpl implements LoginCodeCommandService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginCodeRepository loginCodeRepository;
    private final EmailSender emailSender;
    private final Clock clock;

    public LoginCodeCommandServiceImpl(LoginCodeRepository loginCodeRepository, EmailSender emailSender, Clock clock) {
        this.loginCodeRepository = loginCodeRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /**
     * The limit is checked by counting rows, so two simultaneous requests may both pass at four codes; a sixth
     * code in the hour is accepted as harmless.
     */
    @Override
    public LoginCode handle(RequestLoginCodeCommand command) {
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        var issued = QuarkusTransaction.requiringNew().call(() -> issue(command.email(), code));
        emailSender.sendLoginCode(command.email(), code, issued.getId());
        return issued;
    }

    private LoginCode issue(Email email, String code) {
        var now = clock.instant();
        if (loginCodeRepository.countIssuedSince(email, now.minus(LoginCode.RATE_WINDOW)) >= LoginCode.MAX_PER_WINDOW)
            throw new LoginCodeLimitReachedException();
        return loginCodeRepository.save(LoginCode.issue(email, SecretHash.of(code), now));
    }
}
