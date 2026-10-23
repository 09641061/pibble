package com.takodu.api.shared.testing;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

/**
 * A signed-in Takodu team member, created by {@link TestOperators}.
 *
 * @param accessToken the operator's bearer token
 */
public record TestOperator(String accessToken, String email) {

    /** @return a request with the operator's token and no {@code X-Organization-Id} */
    public RequestSpecification spec() {
        return new RequestSpecBuilder().addHeader("Authorization", "Bearer " + accessToken).build();
    }
}
