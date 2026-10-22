package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
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

    /**
     * @param wabaId the business's WhatsApp Business Account id
     * @param phoneNumberId Meta's id of the number, which webhooks carry
     * @param displayPhone the number as people read it, such as {@code +51 987 654 321}
     */
    public static WhatsappLine connect(OrganizationId organizationId, EstablishmentId establishmentId, String wabaId,
                                       String phoneNumberId, String displayPhone, EncryptedToken accessToken,
                                       LineMode mode, Instant now, IdempotencyKey idempotencyKey) {
        return new WhatsappLine(null, organizationId, establishmentId, wabaId, phoneNumberId, displayPhone, accessToken,
                mode, true, now, idempotencyKey);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public WhatsappLine(WhatsappLineId id, OrganizationId organizationId, EstablishmentId establishmentId,
                        String wabaId, String phoneNumberId, String displayPhone, EncryptedToken accessToken,
                        LineMode mode, boolean connected, Instant connectedAt, IdempotencyKey lastIdempotencyKey) {
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
    }

    /**
     * The owner went through Embedded Signup again with the same number, after it was disconnected.
     *
     * @throws LineAlreadyConnectedException when the line is still connected (W1)
     */
    public void reconnect(String wabaId, String displayPhone, EncryptedToken accessToken, LineMode mode, Instant now,
                          IdempotencyKey idempotencyKey) {
        if (connected) throw new LineAlreadyConnectedException("This number is already connected to the establishment");
        this.wabaId = requireText(wabaId, "wabaId");
        this.displayPhone = requireText(displayPhone, "displayPhone");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken is required");
        this.mode = Objects.requireNonNull(mode, "mode is required");
        this.connected = true;
        this.connectedAt = now;
        this.lastIdempotencyKey = idempotencyKey;
    }

    /** The owner disconnected it, or Meta reported the number removed or banned. Repeating it changes nothing. */
    public void disconnect() {
        connected = false;
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
