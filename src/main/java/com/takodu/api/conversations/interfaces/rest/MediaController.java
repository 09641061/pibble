package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.queryservices.MessageQueryService;
import com.takodu.api.conversations.domain.model.queries.GetMessageMediaQuery;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.application.tenancy.TenantContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.UUID;

@Path("/api/v1/media/{messageId}")
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class MediaController {
    /** Only the member's browser keeps it, for a day. */
    private static final String CACHE = "private, max-age=86400";

    private final MessageQueryService messageQueryService;
    private final TenantContext tenantContext;

    public MediaController(MessageQueryService messageQueryService, TenantContext tenantContext) {
        this.messageQueryService = messageQueryService;
        this.tenantContext = tenantContext;
    }

    /**
     * The file came from a customer, so the browser may not guess its type or run it: only images, audio and video
     * are shown inline, and everything is sandboxed. A message's file never changes, so the message id is its ETag;
     * the access and media checks still run before a 304, so removed media stays 404.
     */
    @GET
    @Operation(summary = "Download a message's media", description = "The image, audio, video, document or sticker a customer sent, once the media job stored it (07 §4). Works while the plan is inactive.")
    @APIResponse(responseCode = "200", description = "The file, with the type WhatsApp reported; cacheable for a day, with an ETag")
    @APIResponse(responseCode = "304", description = "If-None-Match matched: the cached file is current")
    @APIResponse(responseCode = "403", description = "Staff not assigned to its establishment")
    @APIResponse(responseCode = "404", description = "Message not in the organization, or it carries no media; MEDIA_REMOVED once deleted")
    @APIResponse(responseCode = "409", description = "MEDIA_NOT_READY: still being downloaded; try again shortly")
    @APIResponse(responseCode = "422", description = "MEDIA_DOWNLOAD_FAILED: it could not be downloaded")
    public Response download(@PathParam("messageId") UUID messageId, @Context Request request) {
        var media = messageQueryService.handle(new GetMessageMediaQuery(tenantContext.organizationId(),
                new MessageId(messageId)));
        var tag = new EntityTag(messageId.toString());
        var unchanged = request.evaluatePreconditions(tag);
        if (unchanged != null) {
            close(media.content());
            return unchanged.tag(tag).header("Cache-Control", CACHE).build();
        }
        var type = mediaType(media.mimeType());
        var inline = type.getType().equals("image") || type.getType().equals("audio") || type.getType().equals("video");
        return Response.ok(media.content(), type)
                .header("Content-Disposition", inline ? "inline" : "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox")
                .header("Cache-Control", CACHE)
                .tag(tag)
                .build();
    }

    private static void close(InputStream content) {
        try {
            content.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static MediaType mediaType(String mime) {
        try {
            var type = MediaType.valueOf(mime);
            return type.isWildcardType() || type.isWildcardSubtype() ? MediaType.APPLICATION_OCTET_STREAM_TYPE
                    : new MediaType(type.getType().toLowerCase(Locale.ROOT), type.getSubtype(), type.getParameters());
        } catch (IllegalArgumentException | NullPointerException e) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }
}
