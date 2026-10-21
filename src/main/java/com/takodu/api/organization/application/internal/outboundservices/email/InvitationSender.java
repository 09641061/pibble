package com.takodu.api.organization.application.internal.outboundservices.email;

import com.takodu.api.shared.domain.model.valueobjects.Email;

import java.util.UUID;

/** Emails invitation links. Implementations never log the token outside dev mode. */
public interface InvitationSender {

    /**
     * @param token the plain token; the link carries it
     * @param idempotencyKey the provider's key, derived from the token, so a retry sends one email
     */
    void sendInvitation(Email to, String organizationName, String invitedBy, String token, UUID idempotencyKey);
}
