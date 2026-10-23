package com.takodu.api.booking.application.internal.queryservices;

import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.queries.GetAppointmentsByEstablishmentQuery;
import com.takodu.api.booking.domain.model.queries.GetAvailableSlotsQuery;
import com.takodu.api.booking.domain.model.queries.GetUpcomingAppointmentsByCustomerQuery;
import com.takodu.api.booking.domain.model.queries.GetUpcomingAppointmentsByMemberQuery;
import com.takodu.api.booking.domain.model.valueobjects.Slot;
import com.takodu.api.booking.domain.repositories.AppointmentRepository;
import com.takodu.api.booking.domain.repositories.TimeBlockRepository;
import com.takodu.api.booking.domain.services.AvailabilityPolicy;
import com.takodu.api.booking.domain.services.BookingContext;
import com.takodu.api.booking.domain.services.SlotFinder;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AppointmentQueryServiceImpl implements AppointmentQueryService {
    private final AppointmentRepository appointmentRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalCustomersService externalCustomersService;
    private final AccessPolicy accessPolicy;
    private final Clock clock;
    private final SlotFinder slotFinder = new SlotFinder(new AvailabilityPolicy());

    public AppointmentQueryServiceImpl(AppointmentRepository appointmentRepository,
                                       TimeBlockRepository timeBlockRepository,
                                       ExternalOrganizationService externalOrganizationService,
                                       ExternalCustomersService externalCustomersService, AccessPolicy accessPolicy,
                                       Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalCustomersService = externalCustomersService;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public Optional<Appointment> handle(GetAppointmentByIdQuery query) {
        var appointment = appointmentRepository.findById(query.organizationId(), query.appointmentId());
        appointment.ifPresent(found -> accessPolicy.requireEstablishment(found.getEstablishmentId()));
        return appointment;
    }

    @Override
    public List<Appointment> handle(GetAppointmentsByEstablishmentQuery query) {
        externalOrganizationService.fetchCalendar(query.organizationId(), query.establishmentId());
        accessPolicy.requireEstablishment(query.establishmentId());
        return appointmentRepository.findByEstablishment(query.organizationId(), query.establishmentId(), query.window());
    }

    @Override
    public List<Slot> handle(GetAvailableSlotsQuery query) {
        var service = externalOrganizationService.fetchServiceOffer(query.organizationId(), query.serviceId());
        var calendar = externalOrganizationService.fetchCalendar(query.organizationId(), service.establishmentId());
        accessPolicy.requireEstablishment(calendar.establishmentId());
        if (calendar.archived()) throw new ServiceNotBookableException("The establishment is archived");
        if (!service.active()) throw new ServiceNotBookableException("The service is inactive");
        if (query.customerId() != null)
            externalCustomersService.requireActiveCustomer(query.organizationId(), query.customerId());

        var day = calendar.dayOf(query.date());
        var context = new BookingContext(calendar, service,
                timeBlockRepository.findOverlapping(query.organizationId(), calendar.establishmentId(), day),
                appointmentRepository.findOccupying(query.organizationId(), service.performers(), query.customerId(), day),
                clock.instant());
        return slotFinder.find(context, query.date(), query.customerId(), query.staffMemberId(), query.source());
    }

    @Override
    public List<Appointment> handle(GetUpcomingAppointmentsByCustomerQuery query) {
        accessPolicy.requireEstablishment(query.establishmentId());
        return appointmentRepository.findUpcomingByCustomer(query.organizationId(), query.establishmentId(),
                query.customerId(), query.now());
    }

    @Override
    public List<Appointment> handle(GetUpcomingAppointmentsByMemberQuery query) {
        return appointmentRepository.findUpcomingByMember(query.organizationId(), query.memberId(), query.now());
    }
}
