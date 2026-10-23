package com.takodu.api.iam.infrastructure.jwt;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Production refuses to start when a JWT key file cannot be read. SmallRye JWT reads the private key only at the
 * first sign-in, so without this check a wrong path would surface as failed sign-ins after the deploy.
 */
@Startup
@ApplicationScoped
@IfBuildProfile("prod")
public class SigningKeysCheck {

    public SigningKeysCheck(@ConfigProperty(name = "smallrye.jwt.sign.key.location") String privateKey,
                            @ConfigProperty(name = "mp.jwt.verify.publickey.location") String publicKey) {
        requireReadable("smallrye.jwt.sign.key.location", privateKey);
        requireReadable("mp.jwt.verify.publickey.location", publicKey);
    }

    /** @param location a file path, optionally prefixed with {@code file:} */
    static void requireReadable(String property, String location) {
        var path = Path.of(location.startsWith("file:") ? location.substring("file:".length()) : location);
        if (!Files.isRegularFile(path) || !Files.isReadable(path))
            throw new IllegalStateException(property + " points to " + path + ", which is not a readable file");
    }
}
