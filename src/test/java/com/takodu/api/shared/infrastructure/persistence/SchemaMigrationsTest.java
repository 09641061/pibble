package com.takodu.api.shared.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SchemaMigrationsTest {

    @Inject
    Flyway flyway;

    @Test
    void migrationsAreAppliedAtStartup() {
        assertEquals(0, flyway.info().pending().length);
    }

    @Test
    void appliedMigrationsMatchTheFilesOnDisk() {
        assertTrue(flyway.validateWithResult().validationSuccessful);
    }
}
