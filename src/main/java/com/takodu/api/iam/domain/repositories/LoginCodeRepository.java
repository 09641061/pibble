package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.valueobjects.Email;

import java.time.Instant;

/** Sign-in codes; lookups are by the email that requested them. */
public interface LoginCodeRepository {

    /**
     * @return how many codes this email requested at or after {@code since} (I3)
     */
    long countIssuedSince(Email email, Instant since);

    LoginCode save(LoginCode loginCode);
}
