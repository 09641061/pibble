package com.takodu.api.organization.domain.model.valueobjects;

import java.time.ZoneId;

/**
 * O7: the IANA time zone of an establishment. Opening hours and every time shown to people use it; storage is UTC.
 *
 * @param value a region id such as {@code America/Lima}, never a fixed offset
 */
public record IanaZone(ZoneId value) {
    public static final IanaZone LIMA = new IanaZone(ZoneId.of("America/Lima"));

    public IanaZone {
        if (value == null || !ZoneId.getAvailableZoneIds().contains(value.getId()))
            throw new IllegalArgumentException("Time zone must be an IANA name such as America/Lima");
    }

    /**
     * @param name the IANA name; blank means {@link #LIMA}
     * @return the time zone
     */
    public static IanaZone of(String name) {
        if (name == null || name.isBlank()) return LIMA;
        if (!ZoneId.getAvailableZoneIds().contains(name.strip()))
            throw new IllegalArgumentException("Time zone must be an IANA name such as America/Lima");
        return new IanaZone(ZoneId.of(name.strip()));
    }
}
