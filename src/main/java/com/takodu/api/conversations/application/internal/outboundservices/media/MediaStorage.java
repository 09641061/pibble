package com.takodu.api.conversations.application.internal.outboundservices.media;

import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.io.InputStream;
import java.io.UncheckedIOException;

/** Keeps downloaded media files on Takodu's disk, under the configured media directory. */
public interface MediaStorage {

    /**
     * Writes the whole stream, then makes the file visible at once, so a failed write leaves nothing behind.
     *
     * @return where the file is, relative to the media directory; built from the ids only, never from a file name
     * @throws UncheckedIOException when the disk refuses the file
     */
    String store(OrganizationId organizationId, MessageId messageId, InputStream content);

    /**
     * @param path a path {@link #store} returned
     * @return the file's bytes, which the caller closes
     * @throws UncheckedIOException when the file is missing or unreadable
     */
    InputStream open(String path);
}
