package com.takodu.api.iam.domain.repositories;

import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.valueobjects.Email;

import java.time.Instant;
import java.util.List;

/** Sign-in codes; lookups are by the email that requested them. */
public interface LoginCodeRepository {

    /**
     * @return how many codes this email requested at or after {@code since} (I3)
     */
    long countIssuedSince(Email email, Instant since);

    /**
     * Locks the rows until the transaction ends, so two sign-ins with one code cannot both succeed.
     *
     * @return the email's codes that are unused, unexpired and under the attempt limit at {@code now}
     */
    List<LoginCode> findUsableForUpdate(Email email, Instant now);

    LoginCode save(LoginCode loginCode);

    /** @return how many codes that expired before {@code instant} were deleted */
    long deleteExpiredBefore(Instant instant);
}
