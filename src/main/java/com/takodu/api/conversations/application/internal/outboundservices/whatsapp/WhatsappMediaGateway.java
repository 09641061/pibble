package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;

import java.io.InputStream;

/** Downloads a message's media from Meta with the line's business token. Only the media job calls it. */
public interface WhatsappMediaGateway {

    /**
     * Asks Meta for the media's short-lived url and opens the download.
     *
     * @param mediaId Meta's id of the media, from the webhook
     * @param maxBytes a larger file is refused, before the download when Meta tells its size and while reading
     * @return the file's bytes, which the caller reads and closes; reading throws {@link WhatsappMediaException}
     *         past {@code maxBytes}
     * @throws WhatsappMediaException when Meta refuses or does not answer
     */
    InputStream download(WhatsappLine line, String mediaId, long maxBytes);
}
