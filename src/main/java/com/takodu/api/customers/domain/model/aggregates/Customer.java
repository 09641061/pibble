package com.takodu.api.customers.domain.model.aggregates;

import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * C1-C5: a person the organization serves, identified by phone. The name staff typed and the WhatsApp profile name
 * are kept apart, so a new profile name never overwrites what staff typed.
 */
@Getter
public class Customer {
    public static final int MAX_NAME = 120;
    public static final int MAX_NOTES = 2000;

    private final CustomerId id;
    private final OrganizationId organizationId;
    private final PhoneNumber phone;
    private String name;
    private String whatsappProfileName;
    private IdentityDocument document;
    private String notes;
    private Instant deletedAt;
    private final IdempotencyKey idempotencyKey;

    /** C3: staff register a customer by hand; name, document and notes are optional (C4). */
    public static Customer registerByStaff(OrganizationId organizationId, PhoneNumber phone, String name,
                                           IdentityDocument document, String notes, IdempotencyKey idempotencyKey) {
        return new Customer(null, organizationId, phone, name, null, document, notes, null,
                Objects.requireNonNull(idempotencyKey, "idempotencyKey is required"));
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Customer(CustomerId id, OrganizationId organizationId, PhoneNumber phone, String name,
                    String whatsappProfileName, IdentityDocument document, String notes, Instant deletedAt,
                    IdempotencyKey idempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.phone = Objects.requireNonNull(phone, "Phone is required");
        this.name = requireName(name);
        this.whatsappProfileName = whatsappProfileName;
        this.document = document;
        this.notes = requireNotes(notes);
        this.deletedAt = deletedAt;
        this.idempotencyKey = idempotencyKey;
    }

    /** C4: replaces what staff typed; the phone never changes (C1). */
    public void updateDetails(String name, IdentityDocument document, String notes) {
        this.name = requireName(name);
        this.document = document;
        this.notes = requireNotes(notes);
    }

    /** C5: the phone is free again; appointments and conversations keep pointing at this customer. */
    public void delete(Instant now) {
        if (deletedAt == null) deletedAt = Objects.requireNonNull(now, "now is required");
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** The name staff see: the typed name, else the WhatsApp profile name, else the phone. */
    public String displayName() {
        if (name != null) return name;
        return whatsappProfileName != null ? whatsappProfileName : phone.e164();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) return null;
        if (name.strip().length() > MAX_NAME) throw new IllegalArgumentException("Name must have at most 120 characters");
        return name.strip();
    }

    private static String requireNotes(String notes) {
        if (notes == null || notes.isBlank()) return null;
        if (notes.length() > MAX_NOTES) throw new IllegalArgumentException("Notes must have at most 2000 characters");
        return notes.strip();
    }
}
