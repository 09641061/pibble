package com.takodu.api.organization.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IanaZoneTest {

    @Test
    void blankMeansLima() {
        assertEquals(IanaZone.LIMA, IanaZone.of(" "));
    }

    @Test
    void acceptsARegion() {
        assertEquals(ZoneId.of("America/Bogota"), IanaZone.of("America/Bogota").value());
    }

    @Test
    void refusesOffsetsAndUnknownNames() {
        assertThrows(IllegalArgumentException.class, () -> IanaZone.of("-05:00"));
        assertThrows(IllegalArgumentException.class, () -> IanaZone.of("America/Atlantis"));
    }
}
