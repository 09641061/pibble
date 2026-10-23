package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.TemplateStatus;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * W1, W2: the connection between one establishment and one WhatsApp business number. Disconnecting keeps the line
 * and its conversations; connecting the same number again reuses it.
 */
@Getter
public class WhatsappLine {
    private final WhatsappLineId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private String wabaId;
    private final String phoneNumberId;
    private String displayPhone;
    private EncryptedToken accessToken;
    private LineMode mode;
    private boolean connected;
    private Instant connectedAt;
    private IdempotencyKey lastIdempotencyKey;
    private final Map<NotificationTemplate, TemplateStatus> templates = new EnumMap<>(NotificationTemplate.class);

    /**
     * @param templates Meta's review of the notification templates, as signup left them; a template missing from it
     *        could not be created
     * @param wabaId the business's WhatsApp Business Account id
     * @param phoneNumberId Meta's id of the number, which webhooks carry
     * @param displayPhone the number as people read it, such as {@code +51 987 654 321}
     */
    public static WhatsappLine connect(OrganizationId organizationId, EstablishmentId establishmentId, String wabaId,
                                       String phoneNumberId, String displayPhone, EncryptedToken accessToken,
                                       LineMode mode, Map<NotificationTemplate, TemplateStatus> templates,
                                       Instant now, IdempotencyKey idempotencyKey) {
        return new WhatsappLine(null, organizationId, establishmentId, wabaId, phoneNumberId, displayPhone, accessToken,
                mode, true, now, idempotencyKey, templates);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public WhatsappLine(WhatsappLineId id, OrganizationId organizationId, EstablishmentId establishmentId,
                        String wabaId, String phoneNumberId, String displayPhone, EncryptedToken accessToken,
                        LineMode mode, boolean connected, Instant connectedAt, IdempotencyKey lastIdempotencyKey,
                        Map<NotificationTemplate, TemplateStatus> templates) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.wabaId = requireText(wabaId, "wabaId");
        this.phoneNumberId = requireText(phoneNumberId, "phoneNumberId");
        this.displayPhone = requireText(displayPhone, "displayPhone");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken is required");
        this.mode = Objects.requireNonNull(mode, "mode is required");
        this.connected = connected;
        this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt is required");
        this.lastIdempotencyKey = lastIdempotencyKey;
        this.templates.putAll(templates);
    }

    /**
     * The owner went through Embedded Signup again with the same number, after it was disconnected.
     *
     * @throws LineAlreadyConnectedException when the line is still connected (W1)
     */
    public void reconnect(String wabaId, String displayPhone, EncryptedToken accessToken, LineMode mode,
                          Map<NotificationTemplate, TemplateStatus> templates, Instant now,
                          IdempotencyKey idempotencyKey) {
        if (connected) throw new LineAlreadyConnectedException("This number is already connected to the establishment");
        this.wabaId = requireText(wabaId, "wabaId");
        this.displayPhone = requireText(displayPhone, "displayPhone");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken is required");
        this.mode = Objects.requireNonNull(mode, "mode is required");
        this.connected = true;
        this.connectedAt = now;
        this.lastIdempotencyKey = idempotencyKey;
        this.templates.clear();
        this.templates.putAll(templates);
    }

    /** The owner disconnected it, or Meta reported the number removed or banned. Repeating it changes nothing. */
    public void disconnect() {
        connected = false;
    }

    /** Meta reviewed, paused or disabled one of the templates (webhook {@code message_template_status_update}). */
    public void recordTemplateStatus(NotificationTemplate template, TemplateStatus status) {
        templates.put(Objects.requireNonNull(template, "template is required"),
                Objects.requireNonNull(status, "status is required"));
    }

    /** @return Meta's review of each template that exists on the account */
    public Map<NotificationTemplate, TemplateStatus> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    /** @return whether Meta lets the line send the template now; notifications that need it wait otherwise */
    public boolean canSend(NotificationTemplate template) {
        return templates.get(template) == TemplateStatus.APPROVED;
    }

    /** @return whether a connection with this key was already applied, so a retry returns the line */
    public boolean alreadyApplied(IdempotencyKey idempotencyKey) {
        return idempotencyKey.equals(lastIdempotencyKey);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
}
