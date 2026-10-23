package com.takodu.api.iam.application.internal.outboundservices.email;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** The {@link EmailSender} of every {@code @QuarkusTest}: keeps the codes so tests can sign in. */
@Singleton
public class RecordingEmailSender implements EmailSender {
    private final Map<String, List<String>> codes = new ConcurrentHashMap<>();

    @Override
    public void sendLoginCode(Email to, String code, LoginCodeId loginCodeId) {
        codes.computeIfAbsent(to.value(), email -> new CopyOnWriteArrayList<>()).add(code);
    }

    /** @return every code sent to the email, oldest first */
    public List<String> codesSentTo(String email) {
        return codes.getOrDefault(email.toLowerCase(), List.of());
    }

    /** @return the newest code sent to the email */
    public String lastCodeSentTo(String email) {
        var sent = codesSentTo(email);
        if (sent.isEmpty()) throw new AssertionError("No code was sent to " + email);
        return sent.getLast();
    }
}
