package com.takodu.api.assistant.application.internal.tools;

import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalBookingService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.assistant.domain.model.valueobjects.OfferedService;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The slot tool without a model or a database: booking answers a full day from 09:00 to 17:00 every 15 minutes. */
class BookingToolsTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    private static final OfferedService CUT = new OfferedService(new ServiceId(UUID.randomUUID()), "Corte",
            Duration.ofMinutes(60), new Money(3500, "PEN"), null, List.of(new MemberId(UUID.randomUUID())));

    private List<Instant> freeTimes;
    private BookingTools tools;

    @BeforeEach
    void toolsForOneTurn() {
        freeTimes = IntStream.rangeClosed(0, 32)
                .mapToObj(i -> DAY.atTime(9, 0).plusMinutes(15L * i).atZone(LIMA).toInstant()).toList();
        var organizationId = new OrganizationId(UUID.randomUUID());
        var establishmentId = new EstablishmentId(UUID.randomUUID());
        var scope = new TurnScope();
        scope.bind(AssistantTurn.enqueue(organizationId, establishmentId, new ConversationId(UUID.randomUUID()),
                        new CustomerId(UUID.randomUUID()), new MessageId(UUID.randomUUID()), "text", Instant.EPOCH,
                        Instant.EPOCH),
                new EstablishmentProfile(establishmentId, "Centro", null, LIMA, List.of(), false, true), "");
        var organization = new ExternalOrganizationService(null) {
            @Override
            public List<OfferedService> fetchActiveServices(OrganizationId o, EstablishmentId e) {
                return List.of(CUT);
            }

            @Override
            public Optional<String> fetchMemberName(OrganizationId o, MemberId m, EstablishmentId e) {
                return Optional.of("Carla");
            }
        };
        var booking = new ExternalBookingService(null) {
            @Override
            public List<Instant> fetchFreeTimes(OrganizationId o, ServiceId s, CustomerId c, LocalDate d, MemberId m) {
                return freeTimes;
            }
        };
        tools = new BookingTools(scope, organization, booking);
    }

    @Test
    void withoutAPartOfTheDayTheTimesSpreadAcrossItAndSayHowManyMore() {
        assertEquals("Horarios libres el martes 3 de noviembre: 09:00, 10:30, 12:00, 13:45, 15:15, 17:00 y 27 más."
                        + " Para ver otros, busca con una franja o una hora aproximada.",
                tools.findSlots("corte", DAY.toString(), null, null));
    }

    @Test
    void theAfternoonShowsOnlyAfternoonTimes() {
        assertEquals("Horarios libres el martes 3 de noviembre en la tarde: 12:00, 13:00, 14:00, 15:00, 16:00, 17:00"
                + " y 15 más.", tools.findSlots("corte", DAY.toString(), null, "en la tarde"));
    }

    @Test
    void anEmptyPartOfTheDayOffersTheRestOfTheDay() {
        freeTimes = freeTimes.subList(0, 4);
        assertEquals("No hay horarios libres el martes 3 de noviembre en la tarde. Otros horarios ese día: 09:00,"
                + " 09:15, 09:30, 09:45.", tools.findSlots("corte", DAY.toString(), null, "tarde"));
    }

    @Test
    void anApproximateTimeShowsTheNearestOnes() {
        assertEquals("Horarios libres el martes 3 de noviembre cerca de las 16:00: 15:15, 15:30, 15:45, 16:00, 16:15,"
                + " 16:30 y 27 más.", tools.findSlots("corte", DAY.toString(), null, "16:00"));
    }

    @Test
    void anUnknownPartOfTheDayIsExplained() {
        assertEquals("La franja debe ser mañana, tarde o noche, o una hora en formato HH:MM.",
                tools.findSlots("corte", DAY.toString(), null, "madrugada"));
    }

}
