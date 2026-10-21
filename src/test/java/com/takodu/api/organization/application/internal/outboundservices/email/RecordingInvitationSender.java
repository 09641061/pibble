package com.takodu.api.organization.application.internal.outboundservices.email;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** The {@link InvitationSender} of every {@code @QuarkusTest}: keeps the tokens so tests can accept invitations. */
@Singleton
public class RecordingInvitationSender implements InvitationSender {
    private final Map<String, List<String>> tokens = new ConcurrentHashMap<>();

    @Override
    public void sendInvitation(Email to, String organizationName, String invitedBy, String token,
                               UUID idempotencyKey) {
        tokens.computeIfAbsent(to.value(), email -> new CopyOnWriteArrayList<>()).add(token);
    }

    /** @return every token sent to the email, oldest first */
    public List<String> tokensSentTo(String email) {
        return tokens.getOrDefault(email.toLowerCase(), List.of());
    }

    /** @return the newest token sent to the email */
    public String lastTokenSentTo(String email) {
        var sent = tokensSentTo(email);
        if (sent.isEmpty()) throw new AssertionError("No invitation was sent to " + email);
        return sent.getLast();
    }
}
