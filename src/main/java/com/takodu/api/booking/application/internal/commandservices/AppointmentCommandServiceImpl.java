package com.takodu.api.booking.application.internal.commandservices;

import com.takodu.api.booking.application.commandservices.AppointmentCommandService;
import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.booking.domain.exceptions.AppointmentNotFoundException;
import com.takodu.api.booking.domain.exceptions.CustomerChangeTooLateException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.commands.BookAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.CancelAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.ChangeAppointmentStatusCommand;
import com.takodu.api.booking.domain.model.commands.RescheduleAppointmentCommand;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.EstablishmentCalendar;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.booking.domain.repositories.AppointmentRepository;
import com.takodu.api.booking.domain.repositories.TimeBlockRepository;
import com.takodu.api.booking.domain.services.AvailabilityPolicy;
import com.takodu.api.booking.domain.services.BookingContext;
import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentCancelledIntegrationEvent;
import com.takodu.api.booking.interfaces.events.AppointmentRescheduledIntegrationEvent;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class AppointmentCommandServiceImpl implements AppointmentCommandService {
    private final AppointmentRepository appointmentRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalCustomersService externalCustomersService;
    private final AccessPolicy accessPolicy;
    private final Event<AppointmentBookedIntegrationEvent> appointmentBooked;
    private final Event<AppointmentRescheduledIntegrationEvent> appointmentRescheduled;
    private final Event<AppointmentCancelledIntegrationEvent> appointmentCancelled;
    private final Clock clock;
    private final AvailabilityPolicy availabilityPolicy = new AvailabilityPolicy();

    public AppointmentCommandServiceImpl(AppointmentRepository appointmentRepository,
                                         TimeBlockRepository timeBlockRepository,
                                         ExternalOrganizationService externalOrganizationService,
                                         ExternalCustomersService externalCustomersService, AccessPolicy accessPolicy,
                                         Event<AppointmentBookedIntegrationEvent> appointmentBooked,
                                         Event<AppointmentRescheduledIntegrationEvent> appointmentRescheduled,
                                         Event<AppointmentCancelledIntegrationEvent> appointmentCancelled, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalCustomersService = externalCustomersService;
        this.accessPolicy = accessPolicy;
        this.appointmentBooked = appointmentBooked;
        this.appointmentRescheduled = appointmentRescheduled;
        this.appointmentCancelled = appointmentCancelled;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Appointment handle(BookAppointmentCommand command) {
        accessPolicy.require(Action.BOOK_APPOINTMENTS);
        var earlier = appointmentRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var service = externalOrganizationService.fetchServiceOffer(command.organizationId(), command.serviceId());
        var calendar = externalOrganizationService.fetchCalendar(command.organizationId(), service.establishmentId());
        accessPolicy.requireEstablishment(calendar.establishmentId());
        requireBookable(command.organizationId(), calendar, service);
        externalCustomersService.requireActiveCustomer(command.organizationId(), command.customerId());

        var range = TimeRange.of(command.start(), service.duration());
        var candidates = command.staffMemberId() == null ? service.performers() : List.of(command.staffMemberId());
        var context = loadContext(command.organizationId(), calendar, service, candidates, command.customerId(), range);
        var member = command.staffMemberId() == null
                ? availabilityPolicy.chooseMember(context, command.customerId(), range, command.source())
                : command.staffMemberId();
        availabilityPolicy.ensureBookable(context, command.customerId(), member, range, command.source(), null);

        var appointment = appointmentRepository.save(Appointment.book(command.organizationId(), command.customerId(),
                service, member, command.start(), command.source(), command.createdBy(), command.idempotencyKey()));
        appointmentBooked.fire(new AppointmentBookedIntegrationEvent(command.organizationId().value(),
                appointment.getId().value(), appointment.getEstablishmentId().value(), appointment.getCustomerId().value(),
                appointment.getStaffMemberId().value(), appointment.getServiceId().value(), appointment.getRange().start(),
                appointment.getRange().end(), appointment.getSource().name().toLowerCase(Locale.ROOT)));
        return appointment;
    }

    @Override
    @Transactional
    public Appointment handle(RescheduleAppointmentCommand command) {
        var appointment = load(command.organizationId(), command.appointmentId());
        if (appointment.alreadyApplied(command.idempotencyKey())) return appointment;
        requireCustomerMayChange(appointment, command.source());
        var calendar = externalOrganizationService.fetchCalendar(command.organizationId(), appointment.getEstablishmentId());
        if (!calendar.planActive()) throw new PlanInactiveException(command.organizationId().value());
        var service = externalOrganizationService.fetchServiceOffer(command.organizationId(), appointment.getServiceId());
        var member = command.staffMemberId() == null ? appointment.getStaffMemberId() : command.staffMemberId();

        appointment.reschedule(member, command.start(), command.idempotencyKey());
        var context = loadContext(command.organizationId(), calendar, service, List.of(member),
                appointment.getCustomerId(), appointment.getRange());
        availabilityPolicy.ensureBookable(context, appointment.getCustomerId(), member, appointment.getRange(),
                command.source(), appointment.getId());

        var saved = appointmentRepository.save(appointment);
        appointmentRescheduled.fire(new AppointmentRescheduledIntegrationEvent(command.organizationId().value(),
                saved.getId().value(), saved.getEstablishmentId().value(), saved.getCustomerId().value(),
                saved.getStaffMemberId().value(), saved.getServiceId().value(), saved.getRange().start(),
                saved.getRange().end()));
        return saved;
    }

    @Override
    @Transactional
    public Appointment handle(CancelAppointmentCommand command) {
        var appointment = load(command.organizationId(), command.appointmentId());
        if (appointment.alreadyApplied(command.idempotencyKey())) return appointment;
        requireCustomerMayChange(appointment, command.source());
        appointment.cancel(command.reason(), command.cancelledBy(), command.idempotencyKey());
        var saved = appointmentRepository.save(appointment);
        appointmentCancelled.fire(new AppointmentCancelledIntegrationEvent(command.organizationId().value(),
                saved.getId().value(), saved.getEstablishmentId().value(), saved.getCustomerId().value(),
                saved.getServiceId().value(), saved.getRange().start(), saved.getCancelledBy().name().toLowerCase(Locale.ROOT),
                saved.getCancelReason().value()));
        return saved;
    }

    @Override
    @Transactional
    public Appointment handle(ChangeAppointmentStatusCommand command) {
        var appointment = load(command.organizationId(), command.appointmentId());
        if (appointment.alreadyApplied(command.idempotencyKey())) return appointment;
        appointment.changeStatus(command.status(), clock.instant(), command.idempotencyKey());
        return appointmentRepository.save(appointment);
    }

    /** Checks the caller before loading, then their establishment once the appointment names it (O13). */
    private Appointment load(OrganizationId organizationId, AppointmentId appointmentId) {
        accessPolicy.require(Action.BOOK_APPOINTMENTS);
        var appointment = appointmentRepository.findById(organizationId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        accessPolicy.requireEstablishment(appointment.getEstablishmentId());
        return appointment;
    }

    /** B14: staff change an appointment at any time; the assistant only until 2 hours before it starts. */
    private void requireCustomerMayChange(Appointment appointment, BookingSource source) {
        if (source == BookingSource.ASSISTANT && !appointment.customerMayChange(clock.instant()))
            throw new CustomerChangeTooLateException();
    }

    /** B1, B7: what the service and its establishment need before any member or time is looked at. */
    private static void requireBookable(OrganizationId organizationId, EstablishmentCalendar calendar,
                                        ServiceOffer service) {
        if (!calendar.planActive()) throw new PlanInactiveException(organizationId.value());
        if (calendar.archived()) throw new ServiceNotBookableException("The establishment is archived");
        if (!service.active()) throw new ServiceNotBookableException("The service is inactive");
    }

    /**
     * Loads the candidates' appointments for the whole local day, so B17 can count them, and the customer's around
     * the range.
     */
    private BookingContext loadContext(OrganizationId organizationId, EstablishmentCalendar calendar,
                                       ServiceOffer service, List<MemberId> candidates, CustomerId customerId,
                                       TimeRange range) {
        var day = calendar.dayOf(LocalDate.ofInstant(range.start(), calendar.zone()));
        var window = new TimeRange(earliest(day.start(), range.start()), latest(day.end(), range.end()));
        return new BookingContext(calendar, service,
                timeBlockRepository.findOverlapping(organizationId, calendar.establishmentId(), range),
                appointmentRepository.findOccupying(organizationId, candidates, customerId, window), clock.instant());
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
