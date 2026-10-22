package com.takodu.api.booking.application.acl;

import com.takodu.api.booking.application.commandservices.AppointmentCommandService;
import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.exceptions.AppointmentNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.commands.BookAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.CancelAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.RescheduleAppointmentCommand;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.queries.GetAvailableSlotsQuery;
import com.takodu.api.booking.domain.model.queries.GetUpcomingAppointmentsByCustomerQuery;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.booking.interfaces.acl.AppointmentView;
import com.takodu.api.booking.interfaces.acl.BookingContextFacade;
import com.takodu.api.booking.interfaces.acl.SlotView;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class BookingContextFacadeImpl implements BookingContextFacade {
    private final AppointmentCommandService appointmentCommandService;
    private final AppointmentQueryService appointmentQueryService;
    private final Clock clock;

    public BookingContextFacadeImpl(AppointmentCommandService appointmentCommandService,
                                    AppointmentQueryService appointmentQueryService, Clock clock) {
        this.appointmentCommandService = appointmentCommandService;
        this.appointmentQueryService = appointmentQueryService;
        this.clock = clock;
    }

    @Override
    public List<SlotView> fetchSlots(UUID organizationId, UUID serviceId, UUID customerId, LocalDate date,
                                     UUID staffMemberId) {
        return appointmentQueryService.handle(new GetAvailableSlotsQuery(new OrganizationId(organizationId),
                        new ServiceId(serviceId), date, staffMemberId == null ? null : new MemberId(staffMemberId),
                        new CustomerId(customerId), BookingSource.ASSISTANT))
                .stream()
                .map(slot -> new SlotView(slot.range().start(),
                        slot.freeMembers().stream().map(MemberId::value).toList()))
                .toList();
    }

    @Override
    public AppointmentView book(UUID organizationId, UUID customerId, UUID serviceId, UUID staffMemberId,
                                Instant start, UUID idempotencyKey) {
        return view(appointmentCommandService.handle(new BookAppointmentCommand(new OrganizationId(organizationId),
                new CustomerId(customerId), new ServiceId(serviceId),
                staffMemberId == null ? null : new MemberId(staffMemberId), start, BookingSource.ASSISTANT, null,
                new IdempotencyKey(idempotencyKey))));
    }

    @Override
    public List<AppointmentView> fetchUpcomingAppointments(UUID organizationId, UUID establishmentId,
                                                           UUID customerId) {
        return appointmentQueryService.handle(new GetUpcomingAppointmentsByCustomerQuery(
                        new OrganizationId(organizationId), new EstablishmentId(establishmentId),
                        new CustomerId(customerId), clock.instant()))
                .stream()
                .map(this::view)
                .toList();
    }

    @Override
    public AppointmentView reschedule(UUID organizationId, UUID customerId, UUID appointmentId, Instant start,
                                      UUID idempotencyKey) {
        var appointment = customersAppointment(organizationId, customerId, appointmentId);
        return view(appointmentCommandService.handle(new RescheduleAppointmentCommand(appointment.getOrganizationId(),
                appointment.getId(), null, start, BookingSource.ASSISTANT, new IdempotencyKey(idempotencyKey))));
    }

    @Override
    public AppointmentView cancel(UUID organizationId, UUID customerId, UUID appointmentId, String reason,
                                  UUID idempotencyKey) {
        var appointment = customersAppointment(organizationId, customerId, appointmentId);
        return view(appointmentCommandService.handle(new CancelAppointmentCommand(appointment.getOrganizationId(),
                appointment.getId(), new CancelReason(reason), CancelledBy.CUSTOMER, BookingSource.ASSISTANT,
                new IdempotencyKey(idempotencyKey))));
    }

    /** A4: another customer's appointment is reported as missing, so its existence is not revealed. */
    private Appointment customersAppointment(UUID organizationId, UUID customerId, UUID appointmentId) {
        var id = new AppointmentId(appointmentId);
        return appointmentQueryService.handle(new GetAppointmentByIdQuery(new OrganizationId(organizationId), id))
                .filter(found -> found.getCustomerId().value().equals(customerId))
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    private AppointmentView view(Appointment appointment) {
        return new AppointmentView(appointment.getId().value(), appointment.getEstablishmentId().value(),
                appointment.getServiceId().value(), appointment.getStaffMemberId().value(),
                appointment.getRange().start(), appointment.getRange().end(),
                appointment.getStatus().name().toLowerCase(Locale.ROOT), appointment.customerMayChange(clock.instant()));
    }
}
