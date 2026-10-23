package com.takodu.api.shared.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

/** Load balancers and the container runtime call the health endpoints without a token or organization. */
@QuarkusTest
class HealthEndpointsTest {

    @ConfigProperty(name = "takodu.media.dir")
    Path mediaDir;

    @BeforeEach
    void mountMedia() throws Exception {
        Files.createDirectories(mediaDir);
    }

    @Test
    void readinessChecksTheDatabaseAndTheMediaVolumeWithoutAToken() {
        given().get("/q/health/ready").then().statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItems("Database connections health check", "media-volume"));
    }

    @Test
    void livenessAnswersWithoutAToken() {
        given().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void tenantFilterLeavesHealthAlone() {
        given().header("X-Organization-Id", "not-a-uuid")
                .get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
    }
}
