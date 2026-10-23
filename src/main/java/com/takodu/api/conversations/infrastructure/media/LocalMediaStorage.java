package com.takodu.api.conversations.infrastructure.media;

import com.takodu.api.conversations.application.internal.outboundservices.media.MediaStorage;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Files under {@code takodu.media.dir} (the media volume in production), at {@code <organization>/<message>}. The
 * mime type stays on the message, so the name needs no extension.
 */
@ApplicationScoped
public class LocalMediaStorage implements MediaStorage {
    private final Path root;

    public LocalMediaStorage(@ConfigProperty(name = "takodu.media.dir") Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String store(OrganizationId organizationId, MessageId messageId, InputStream content) {
        var relative = organizationId.value() + "/" + messageId.value();
        var target = root.resolve(relative);
        Path partial = null;
        try {
            Files.createDirectories(target.getParent());
            partial = Files.createTempFile(target.getParent(), messageId.value().toString(), ".part");
            Files.copy(content, partial, StandardCopyOption.REPLACE_EXISTING);
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return relative;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(partial);
        }
    }

    @Override
    public InputStream open(String path) {
        var file = root.resolve(path).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("The media path leaves the media directory");
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A partial file is left only when the copy failed; after the move it no longer exists. */
    private static void deleteQuietly(Path partial) {
        if (partial == null) return;
        try {
            Files.deleteIfExists(partial);
        } catch (IOException ignored) {
            // a stray partial file is harmless and never served
        }
    }
}
