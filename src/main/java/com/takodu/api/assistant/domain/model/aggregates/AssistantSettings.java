package com.takodu.api.assistant.domain.model.aggregates;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.util.Objects;

/** A1: whether the assistant answers an establishment's customers, and what the owner wants it to know. */
@Getter
public class AssistantSettings {
    /** A1: the business info is free text of at most this many characters. */
    public static final int MAX_BUSINESS_INFO = 4000;

    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private boolean enabled;
    private String businessInfo;

    /** @return the settings of an establishment nobody configured yet: off, with no business info */
    public static AssistantSettings defaultsFor(OrganizationId organizationId, EstablishmentId establishmentId) {
        return new AssistantSettings(organizationId, establishmentId, false, "");
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public AssistantSettings(OrganizationId organizationId, EstablishmentId establishmentId, boolean enabled,
                             String businessInfo) {
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.enabled = enabled;
        this.businessInfo = Objects.requireNonNull(businessInfo, "businessInfo is required");
    }

    /**
     * A1: turns the assistant on or off and replaces the business info.
     *
     * @param businessInfo address details, payment methods, policies, FAQs; null clears it
     * @throws IllegalArgumentException when the business info is longer than 4,000 characters
     */
    public void configure(boolean enabled, String businessInfo) {
        var info = businessInfo == null ? "" : businessInfo.strip();
        if (info.length() > MAX_BUSINESS_INFO)
            throw new IllegalArgumentException("Business info has at most 4000 characters");
        this.enabled = enabled;
        this.businessInfo = info;
    }
}
