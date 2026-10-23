package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestInbox;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappMediaGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.conversations.ConversationRows.messagesWithWamid;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C5 and 07 §4: the retention job deletes files, and the inbox and the media endpoint say so. */
@QuarkusTest
class MediaRetentionJobTest {
    private static final byte[] PHOTO = "a photo of a haircut".getBytes(StandardCharsets.UTF_8);

    @Inject
    TestClock clock;

    @Inject
    MediaDownloader downloader;

    @Inject
    MediaRetentionJob retention;

    @Inject
    RecordingWhatsappMediaGateway media;

    @ConfigProperty(name = "takodu.media.dir")
    Path mediaDir;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void deletingACustomerRemovesTheirFilesOnly() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var deleted = downloaded(customerSendsPhoto(line));
        var kept = downloaded(customerSendsPhoto(line));
        var deletedFile = mediaDir.resolve(deleted.path());

        deleteCustomerOf(salon.owner(), deleted);
        assertEquals("removing", row(deleted).get("media_status"));
        TestInbox.messages(salon.owner(), deleted.conversationId()).body("items[0].mediaStatus", equalTo("removed"));
        assertTrue(Files.exists(deletedFile), "the request deletes no file; the job does");

        removeUntilRemoved(deleted);

        assertEquals("removed", row(deleted).get("media_status"));
        assertNull(row(deleted).get("media_path"));
        assertFalse(Files.exists(deletedFile));
        media(salon.owner(), deleted.messageId()).statusCode(404).body("code", equalTo("MEDIA_REMOVED"));
        assertEquals("ready", row(kept).get("media_status"));
        assertTrue(Files.exists(mediaDir.resolve(kept.path())));
        media(salon.owner(), kept.messageId()).statusCode(200);
    }

    @Test
    void mediaOlderThanTheRetentionPeriodIsRemoved() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var photo = downloaded(customerSendsPhoto(line));
        var file = mediaDir.resolve(photo.path());

        clock.advance(Duration.ofDays(89));
        retention.removeDue();
        assertEquals("ready", row(photo).get("media_status"));

        clock.advance(Duration.ofDays(2));
        removeUntilRemoved(photo);

        assertFalse(Files.exists(file));
        TestInbox.messages(salon.owner(), photo.conversationId()).body("items[0].mediaStatus", equalTo("removed"));
    }

    @Test
    void aDeletedCustomersPendingDownloadIsNeverFetched() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var photo = customerSendsPhoto(line);

        deleteCustomerOf(salon.owner(), photo);
        removeUntilRemoved(photo);
        downloader.downloadDue();

        assertEquals("removed", row(photo).get("media_status"));
        assertEquals(0, media.downloadsOf(photo.mediaId()));
    }

    private record Photo(String wamid, String mediaId, UUID messageId, UUID conversationId, String path) { }

    private Photo customerSendsPhoto(String line) {
        var mediaId = "media-" + UUID.randomUUID();
        media.serve(mediaId, PHOTO);
        var wamid = MetaWebhooks.newWamid();
        MetaWebhooks.post(MetaWebhooks.payload("image-message", Map.of("PHONE_NUMBER_ID", line,
                "WA_ID", MetaWebhooks.newWaId(), "WAMID", wamid, "MEDIA_ID", mediaId))).statusCode(200);
        var row = messagesWithWamid(wamid).getFirst();
        return new Photo(wamid, mediaId, (UUID) row.get("id"), (UUID) row.get("conversation_id"), null);
    }

    /** Media other tests left pending is due too, so the job runs until this photo is stored. */
    private Photo downloaded(Photo photo) {
        for (int run = 0; run < 50 && "pending".equals(row(photo).get("media_status")); run++)
            downloader.downloadDue();
        assertEquals("ready", row(photo).get("media_status"));
        return new Photo(photo.wamid(), photo.mediaId(), photo.messageId(), photo.conversationId(),
                (String) row(photo).get("media_path"));
    }

    /** Other tests' media may be due for removal too, so the job runs until this photo's turn came. */
    private void removeUntilRemoved(Photo photo) {
        for (int run = 0; run < 50 && !"removed".equals(row(photo).get("media_status")); run++)
            retention.removeDue();
    }

    private static void deleteCustomerOf(TestTenant owner, Photo photo) {
        String customerId = TestInbox.conversation(owner, photo.conversationId()).extract().path("customerId");
        given().spec(owner.spec()).delete("/api/v1/customers/" + customerId).then().statusCode(204);
    }

    private static Map<String, Object> row(Photo photo) {
        return messagesWithWamid(photo.wamid()).getFirst();
    }

    private static ValidatableResponse media(TestTenant tenant, UUID messageId) {
        return given().spec(tenant.spec()).get("/api/v1/media/" + messageId).then();
    }
}
