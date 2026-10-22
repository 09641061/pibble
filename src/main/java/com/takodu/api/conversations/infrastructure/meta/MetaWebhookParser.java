package com.takodu.api.conversations.infrastructure.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takodu.api.conversations.domain.model.commands.DisconnectAccountLinesCommand;
import com.takodu.api.conversations.domain.model.commands.RecordBusinessAppEchoCommand;
import com.takodu.api.conversations.domain.model.commands.RecordDeliveryStatusCommand;
import com.takodu.api.conversations.domain.model.commands.RecordHistoryMessageCommand;
import com.takodu.api.conversations.domain.model.commands.RecordInboundMessageCommand;
import com.takodu.api.conversations.domain.model.commands.WhatsappWebhookCommand;
import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The only class that knows the JSON of Meta's webhooks (fields {@code messages}, {@code smb_message_echoes},
 * {@code history}, {@code account_update}, {@code account_offboarded}), checked against Meta's docs on 2026-10-21.
 * A part it cannot read is skipped with a log line that names no number and no text.
 */
public final class MetaWebhookParser {
    private static final Pattern WA_ID = Pattern.compile("\\d{6,15}");
    private static final Set<String> MEDIA = Set.of("image", "audio", "video", "document", "sticker");
    private static final Set<String> DISCONNECTING_EVENTS = Set.of("PARTNER_REMOVED", "ACCOUNT_DELETED");

    private final ObjectMapper objectMapper;

    public MetaWebhookParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** @throws IllegalArgumentException when the body is not JSON */
    public List<WhatsappWebhookCommand> parse(byte[] body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalArgumentException("The webhook body is not JSON");
        }
        var facts = new ArrayList<WhatsappWebhookCommand>();
        if (root == null || !"whatsapp_business_account".equals(root.path("object").asText())) return facts;
        for (var entry : root.path("entry")) {
            var wabaId = entry.path("id").asText();
            for (var change : entry.path("changes")) {
                var value = change.path("value");
                var phoneNumberId = value.path("metadata").path("phone_number_id").asText();
                switch (change.path("field").asText()) {
                    case "messages" -> {
                        readMessages(phoneNumberId, value, facts);
                        readStatuses(value, facts);
                    }
                    case "smb_message_echoes" -> readEchoes(phoneNumberId, value, facts);
                    case "history" -> readHistory(phoneNumberId, value, facts);
                    case "account_update" -> {
                        if (DISCONNECTING_EVENTS.contains(value.path("event").asText()) && !wabaId.isEmpty())
                            facts.add(new DisconnectAccountLinesCommand(wabaId));
                    }
                    case "account_offboarded" -> {
                        if (!wabaId.isEmpty()) facts.add(new DisconnectAccountLinesCommand(wabaId));
                    }
                    default -> { }
                }
            }
        }
        return facts;
    }

    private void readMessages(String phoneNumberId, JsonNode value, List<WhatsappWebhookCommand> facts) {
        Map<String, String> profileNames = new HashMap<>();
        for (var contact : value.path("contacts"))
            profileNames.put(contact.path("wa_id").asText(), text(contact.path("profile").path("name")));
        for (var message : value.path("messages")) {
            var from = message.path("from").asText();
            var wamid = message.path("id").asText();
            if (!readable(phoneNumberId, from, wamid)) continue;
            facts.add(new RecordInboundMessageCommand(phoneNumberId, from, profileNames.get(from), new Wamid(wamid),
                    content(message), timestamp(message)));
        }
    }

    private void readStatuses(JsonNode value, List<WhatsappWebhookCommand> facts) {
        for (var status : value.path("statuses")) {
            var wamid = status.path("id").asText();
            var next = switch (status.path("status").asText()) {
                case "sent" -> DeliveryStatus.SENT;
                case "delivered" -> DeliveryStatus.DELIVERED;
                case "read" -> DeliveryStatus.READ;
                case "failed" -> DeliveryStatus.FAILED;
                default -> null;
            };
            if (wamid.isEmpty() || next == null) continue;
            var pricing = status.path("pricing");
            var error = status.path("errors").path(0);
            facts.add(new RecordDeliveryStatusCommand(new Wamid(wamid), next, text(pricing.path("category")),
                    pricing.has("billable") ? pricing.path("billable").asBoolean() : null,
                    error.isMissingNode() ? null : (error.path("code").asText() + ": " + error.path("title").asText()).strip()));
        }
    }

    private void readEchoes(String phoneNumberId, JsonNode value, List<WhatsappWebhookCommand> facts) {
        for (var echo : value.path("message_echoes")) {
            var to = echo.path("to").asText();
            var wamid = echo.path("id").asText();
            if (!readable(phoneNumberId, to, wamid)) continue;
            facts.add(new RecordBusinessAppEchoCommand(phoneNumberId, to, new Wamid(wamid), content(echo), timestamp(echo)));
        }
    }

    private void readHistory(String phoneNumberId, JsonNode value, List<WhatsappWebhookCommand> facts) {
        for (var chunk : value.path("history")) {
            for (var thread : chunk.path("threads")) {
                var customer = thread.path("id").asText();
                for (var message : thread.path("messages")) {
                    var wamid = message.path("id").asText();
                    if (!readable(phoneNumberId, customer, wamid)) continue;
                    facts.add(new RecordHistoryMessageCommand(phoneNumberId, customer,
                            customer.equals(message.path("from").asText()), new Wamid(wamid), content(message),
                            historyStatus(message.path("history_context").path("status").asText()), timestamp(message)));
                }
            }
        }
    }

    private static boolean readable(String phoneNumberId, String waId, String wamid) {
        if (!phoneNumberId.isEmpty() && WA_ID.matcher(waId).matches() && !wamid.isEmpty() && wamid.length() <= 256)
            return true;
        Log.warnf("Skipped a webhook message without a readable sender or id (wamid %s)", wamid);
        return false;
    }

    /** Interactive replies keep the title the customer tapped; media keep Meta's id for the downloader. */
    private static MessageContent content(JsonNode message) {
        var type = message.path("type").asText();
        var payload = message.path(type);
        if (MEDIA.contains(type)) {
            return new MessageContent(MessageKind.valueOf(type.toUpperCase(Locale.ROOT)), text(payload.path("caption")),
                    text(payload.path("id")), text(payload.path("mime_type")));
        }
        return switch (type) {
            case "text" -> MessageContent.text(text(payload.path("body")));
            case "interactive" -> new MessageContent(MessageKind.INTERACTIVE, text(payload.path(
                    payload.path("type").asText()).path("title")), null, null);
            case "button" -> new MessageContent(MessageKind.BUTTON, text(payload.path("text")), null, null);
            case "reaction" -> new MessageContent(MessageKind.REACTION, text(payload.path("emoji")), null, null);
            case "location" -> new MessageContent(MessageKind.LOCATION, location(payload), null, null);
            default -> new MessageContent(MessageKind.UNSUPPORTED, null, null, null);
        };
    }

    private static String location(JsonNode location) {
        var place = String.join(", ", Stream.of(text(location.path("name")), text(location.path("address")))
                .filter(Objects::nonNull).toList());
        var point = location.path("latitude").asText() + "," + location.path("longitude").asText();
        return place.isEmpty() ? point : place + " (" + point + ")";
    }

    private static DeliveryStatus historyStatus(String status) {
        return switch (status) {
            case "DELIVERED" -> DeliveryStatus.DELIVERED;
            case "READ", "PLAYED" -> DeliveryStatus.READ;
            case "ERROR" -> DeliveryStatus.FAILED;
            default -> DeliveryStatus.SENT;
        };
    }

    /** Meta sends Unix seconds as text. */
    private static Instant timestamp(JsonNode message) {
        return Instant.ofEpochSecond(message.path("timestamp").asLong());
    }

    private static String text(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.asText().isBlank() ? null : node.asText();
    }
}
