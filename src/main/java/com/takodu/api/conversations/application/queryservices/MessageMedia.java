package com.takodu.api.conversations.application.queryservices;

import java.io.InputStream;

/**
 * A downloaded media file, ready to stream.
 *
 * @param mimeType as WhatsApp reported it, such as {@code image/jpeg}
 * @param content the file's bytes, which the caller closes
 */
public record MessageMedia(String mimeType, InputStream content) { }
