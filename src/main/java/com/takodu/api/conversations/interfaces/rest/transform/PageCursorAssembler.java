package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Cursors are opaque to clients: the position of the last row, in base64url. */
public final class PageCursorAssembler {
    private PageCursorAssembler() { }

    /** @throws IllegalArgumentException when the text is not a cursor this API issued */
    public static PageCursor toCursorFromToken(String name, String token) {
        if (token == null || token.isBlank()) return null;
        try {
            var parts = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8).split("\\|", 2);
            return new PageCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(name + " is not a valid cursor");
        }
    }

    public static String toTokenFromCursor(Instant at, UUID id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((at + "|" + id).getBytes(StandardCharsets.UTF_8));
    }
}
