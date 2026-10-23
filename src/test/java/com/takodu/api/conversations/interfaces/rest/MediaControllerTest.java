package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.jobs.MediaDownloader;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappMediaGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.conversations.ConversationRows.messagesWithWamid;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@QuarkusTest
class MediaControllerTest {
    private static final byte[] PHOTO = "a photo of a haircut".getBytes(StandardCharsets.UTF_8);

    @Inject
    TestClock clock;

    @Inject
    MediaDownloader downloader;

    @Inject
    RecordingWhatsappMediaGateway media;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffSeeTheDownloadedPhotoWithItsType() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        var photo = customerSendsPhoto(salon, salon.establishmentId(), PHOTO);
        download(photo);

        var bytes = media(staff, photo.messageId()).statusCode(200)
                .header("Content-Type", startsWith("image/jpeg"))
                .header("Content-Disposition", equalTo("inline"))
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .extract().asByteArray();
        assertArrayEquals(PHOTO, bytes);
        messages(staff, photo.conversationId()).body("items[0].mediaStatus", equalTo("ready"))
                .body("items[0].hasMedia", equalTo(true));
    }

    @Test
    void photoStillDownloadingIsNotReady() {
        var salon = TestSalon.open();
        var photo = customerSendsPhoto(salon, salon.establishmentId(), PHOTO);

        media(salon.owner(), photo.messageId()).statusCode(409).body("code", equalTo("MEDIA_NOT_READY"));
        messages(salon.owner(), photo.conversationId()).body("items[0].mediaStatus", equalTo("pending"));
    }

    @Test
    void photoThatCouldNotBeDownloadedSaysSo() {
        var salon = TestSalon.open();
        var photo = customerSendsPhoto(salon, salon.establishmentId(), new byte[5 * 1024 * 1024 + 1]);
        download(photo);

        media(salon.owner(), photo.messageId()).statusCode(422).body("code", equalTo("MEDIA_DOWNLOAD_FAILED"));
        messages(salon.owner(), photo.conversationId()).body("items[0].mediaStatus", equalTo("failed"));
    }

    @Test
    void textMessageHasNoMedia() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(),
                salon.establishmentId(), line, MetaWebhooks.newWaId(), Instant.now());
        String text = messages(salon.owner(), conversation).extract().path("items[0].id");

        media(salon.owner(), UUID.fromString(text)).statusCode(404).body("code", equalTo("MESSAGE_NOT_FOUND"));
    }

    @Test
    void staffOfAnotherEstablishmentAreRefusedAndAnotherOrganizationFindsNothing() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var photo = customerSendsPhoto(salon, surco, PHOTO);
        download(photo);

        media(salon.hireStaff(), photo.messageId()).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        media(TestTenants.owner(), photo.messageId()).statusCode(404);
        media(salon.owner(), photo.messageId()).statusCode(200);
    }

    @Test
    void mediaIsServedWhileThePlanIsInactive() {
        var salon = TestSalon.open();
        var photo = customerSendsPhoto(salon, salon.establishmentId(), PHOTO);
        download(photo);
        clock.advance(Duration.ofDays(15));

        media(salon.owner(), photo.messageId()).statusCode(200);
    }

    private record Photo(String wamid, UUID messageId, UUID conversationId) { }

    private Photo customerSendsPhoto(TestSalon salon, UUID establishmentId, byte[] content) {
        var line = TestWhatsapp.connectLine(salon.owner(), establishmentId);
        var mediaId = "media-" + UUID.randomUUID();
        media.serve(mediaId, content);
        var wamid = MetaWebhooks.newWamid();
        MetaWebhooks.post(MetaWebhooks.payload("image-message", Map.of("PHONE_NUMBER_ID", line,
                "WA_ID", MetaWebhooks.newWaId(), "WAMID", wamid, "MEDIA_ID", mediaId))).statusCode(200);
        var row = messagesWithWamid(wamid).getFirst();
        return new Photo(wamid, (UUID) row.get("id"), (UUID) row.get("conversation_id"));
    }

    /** Media other tests left pending is due too, so the job runs until this photo's attempt is recorded. */
    private void download(Photo photo) {
        for (int run = 0; run < 50 && "pending".equals(messagesWithWamid(photo.wamid()).getFirst().get("media_status"));
             run++)
            downloader.downloadDue();
    }

    private static ValidatableResponse media(TestTenant tenant, UUID messageId) {
        return given().spec(tenant.spec()).get("/api/v1/media/" + messageId).then();
    }
}
