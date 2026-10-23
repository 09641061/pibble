package com.takodu.api.conversations.infrastructure.media;

import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MediaVolumeHealthCheckTest {

    @TempDir
    Path dir;

    @Test
    void writableDirectoryIsUpAndLeavesNoFile() throws Exception {
        assertEquals(Status.UP, new MediaVolumeHealthCheck(dir).call().getStatus());
        try (var files = Files.list(dir)) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    @Test
    void missingDirectoryIsDown() {
        var response = new MediaVolumeHealthCheck(dir.resolve("not-mounted")).call();

        assertEquals(Status.DOWN, response.getStatus());
        assertEquals("missing", response.getData().orElseThrow().get("problem"));
    }

    @Test
    void readOnlyDirectoryIsDown() throws Exception {
        assumeFalse("root".equals(System.getProperty("user.name")), "root writes anywhere");
        var readOnly = Files.createDirectory(dir.resolve("read-only"));
        Files.setPosixFilePermissions(readOnly, PosixFilePermissions.fromString("r-xr-xr-x"));

        var response = new MediaVolumeHealthCheck(readOnly).call();

        assertEquals(Status.DOWN, response.getStatus());
        assertEquals("not writable", response.getData().orElseThrow().get("problem"));
    }
}
