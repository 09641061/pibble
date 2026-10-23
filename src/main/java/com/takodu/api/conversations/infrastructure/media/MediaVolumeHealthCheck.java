package com.takodu.api.conversations.infrastructure.media;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Readiness of {@code takodu.media.dir}: the directory exists and accepts a file. A missing directory in production
 * means the media volume is not mounted, and downloads would land in the container's own disk.
 */
@Readiness
@ApplicationScoped
public class MediaVolumeHealthCheck implements HealthCheck {
    static final String NAME = "media-volume";

    private final Path root;

    public MediaVolumeHealthCheck(@ConfigProperty(name = "takodu.media.dir") Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public HealthCheckResponse call() {
        var response = HealthCheckResponse.named(NAME);
        if (!Files.isDirectory(root)) return response.down().withData("problem", "missing").build();
        try {
            Files.delete(Files.createTempFile(root, ".health", ".probe"));
            return response.up().build();
        } catch (IOException e) {
            return response.down().withData("problem", "not writable").build();
        }
    }
}
