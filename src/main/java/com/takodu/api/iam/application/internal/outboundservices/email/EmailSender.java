package com.takodu.api.iam.application.internal.outboundservices.email;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;

/** Sends transactional email. Implementations never log the code. */
public interface EmailSender {

    /**
     * @param to the recipient
     * @param code the plain 6-digit code
     * @param loginCodeId the stored code, used as the provider's idempotency key so a retry sends one email
     */
    void sendLoginCode(Email to, String code, LoginCodeId loginCodeId);
}
