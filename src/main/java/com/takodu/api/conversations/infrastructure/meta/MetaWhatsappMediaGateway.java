package com.takodu.api.conversations.infrastructure.meta;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaException;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaGateway;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads media in two steps, checked against Meta's docs on 2026-10-22: {@code GET /{media-id}} returns a url that
 * expires after 5 minutes, which is fetched with the same business token. The token is decrypted here and nowhere
 * else; neither it nor the url appears in an error.
 */
@ApplicationScoped
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "true", enableIfMissing = true)
public class MetaWhatsappMediaGateway implements WhatsappMediaGateway {
    static final String TOO_LARGE = "The file is larger than WhatsApp allows";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final MetaGraphClient metaGraphClient;
    private final TokenCipher tokenCipher;
    private final String version;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public MetaWhatsappMediaGateway(@RestClient MetaGraphClient metaGraphClient, TokenCipher tokenCipher,
                                    @ConfigProperty(name = "takodu.meta.graph-version") String version) {
        this.metaGraphClient = metaGraphClient;
        this.tokenCipher = tokenCipher;
        this.version = version;
    }

    @Override
    public InputStream download(WhatsappLine line, String mediaId, long maxBytes) {
        var authorization = "Bearer " + tokenCipher.decrypt(line.getAccessToken().value());
        MetaGraphClient.MediaResponse media;
        try {
            media = metaGraphClient.fetchMedia(version, mediaId, line.getPhoneNumberId(), authorization);
        } catch (WebApplicationException e) {
            var status = e.getResponse().getStatus();
            throw new WhatsappMediaException("Meta refused the media: HTTP " + status, retryable(status));
        } catch (ProcessingException e) {
            throw new WhatsappMediaException("Meta did not answer", true);
        }
        if (media.fileSize() != null && media.fileSize() > maxBytes) throw new WhatsappMediaException(TOO_LARGE, false);
        if (media.url() == null || !media.url().startsWith("https://"))
            throw new WhatsappMediaException("Meta returned no download url", true);
        return open(URI.create(media.url()), authorization, maxBytes);
    }

    private InputStream open(URI url, String authorization, long maxBytes) {
        var request = HttpRequest.newBuilder(url).timeout(TIMEOUT).header("Authorization", authorization).GET().build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new WhatsappMediaException("Meta did not answer the download", true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhatsappMediaException("The download was interrupted", true);
        }
        if (response.statusCode() != 200) {
            close(response);
            throw new WhatsappMediaException("Meta refused the download: HTTP " + response.statusCode(),
                    retryable(response.statusCode()));
        }
        var length = response.headers().firstValueAsLong("Content-Length");
        if (length.isPresent() && length.getAsLong() > maxBytes) {
            close(response);
            throw new WhatsappMediaException(TOO_LARGE, false);
        }
        return new Limited(response.body(), maxBytes);
    }

    private static void close(HttpResponse<InputStream> response) {
        try {
            response.body().close();
        } catch (IOException ignored) {
            // the download is abandoned either way
        }
    }

    /** A 404 means the url expired: a new one is asked for at the next attempt, as Meta's docs say. */
    static boolean retryable(int status) {
        return status == 404 || status == 408 || status == 429 || status >= 500;
    }

    /** Stops reading past the limit, for a server that did not say the size up front. */
    static final class Limited extends FilterInputStream {
        private final long maxBytes;
        private long read;

        Limited(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            var b = super.read();
            if (b >= 0) count(1);
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            var n = super.read(buffer, offset, length);
            if (n > 0) count(n);
            return n;
        }

        private void count(long n) {
            read += n;
            if (read > maxBytes) throw new WhatsappMediaException(TOO_LARGE, false);
        }
    }
}
