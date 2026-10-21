package com.takodu.api.iam.infrastructure.jwt;

import com.takodu.api.iam.application.internal.outboundservices.tokens.AccessToken;
import com.takodu.api.iam.application.internal.outboundservices.tokens.AccessTokenIssuer;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

/** Signs RS256 access tokens with SmallRye JWT; {@code quarkus-smallrye-jwt} verifies them on each request. */
@ApplicationScoped
public class SmallRyeAccessTokenIssuer implements AccessTokenIssuer {
    private final String issuer;

    public SmallRyeAccessTokenIssuer(@ConfigProperty(name = "mp.jwt.verify.issuer") String issuer) {
        this.issuer = issuer;
    }

    @Override
    public AccessToken issue(UserId userId, SessionId sessionId, Instant now) {
        var expiresAt = now.plus(TTL);
        var token = Jwt.issuer(issuer)
                .subject(userId.value().toString())
                .claim("sid", sessionId.value().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .sign();
        return new AccessToken(token, expiresAt);
    }
}
