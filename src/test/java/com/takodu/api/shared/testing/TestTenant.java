package com.takodu.api.shared.testing;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import java.util.UUID;

/**
 * A signed-in member of an organization, created through the real endpoints by {@link TestTenants}.
 *
 * @param accessToken the member's bearer token
 */
public record TestTenant(String accessToken, String email, UUID organizationId, UUID memberId) {

    /** @return a request with the member's token and {@code X-Organization-Id} */
    public RequestSpecification spec() {
        return new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Organization-Id", organizationId.toString())
                .build();
    }
}
