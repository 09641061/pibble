package com.takodu.api.booking.infrastructure.persistence.jpa.adapters;

import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.booking.domain.repositories.AppointmentRepository;
import com.takodu.api.booking.interfaces.rest.AppointmentsControllerTest;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The exclusion constraints hold even when a write skips {@code AvailabilityPolicy} (B5, B6). */
@QuarkusTest
class AppointmentRepositoryImplTest {

    @Inject
    AppointmentRepository appointmentRepository;

    @Inject
    TestClock clock;

    @Test
    void databaseRefusesAnOverlapOfTheMember() {
        var salon = TestSalon.open();
        var ten = TestSalon.at(clock, 1, "10:00");
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, salon.owner().memberId(), ten)).statusCode(201);

        var refused = assertThrows(SlotUnavailableException.class, () -> save(salon, salon.newCustomer(),
                ten.plus(Duration.ofMinutes(59))));
        assertEquals("The member is busy then", refused.getMessage());
    }

    @Test
    void databaseRefusesAnOverlapOfTheCustomer() {
        var salon = TestSalon.open();
        var ten = TestSalon.at(clock, 1, "10:00");
        var staff = salon.hireStaff();
        salon.performedBy(salon.owner(), staff);
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, staff.memberId(), ten)).statusCode(201);

        var refused = assertThrows(SlotUnavailableException.class, () -> save(salon, salon.customerId(), ten));
        assertEquals("The customer has another appointment then", refused.getMessage());
    }

    /** Writes an appointment of the owner directly, in its own transaction. */
    private void save(TestSalon salon, UUID customerId, Instant start) {
        var service = new ServiceOffer(new ServiceId(salon.serviceId()), new EstablishmentId(salon.establishmentId()),
                Duration.ofHours(1), Money.pen(3500), true, List.of(new MemberId(salon.owner().memberId())));
        QuarkusTransaction.requiringNew().run(() -> appointmentRepository.save(Appointment.book(
                new OrganizationId(salon.owner().organizationId()), new CustomerId(customerId), service,
                new MemberId(salon.owner().memberId()), start, BookingSource.ASSISTANT, null,
                new IdempotencyKey(UUID.randomUUID()))));
    }
}
