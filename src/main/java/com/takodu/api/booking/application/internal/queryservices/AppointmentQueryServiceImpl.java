package com.takodu.api.booking.application.internal.queryservices;

import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.queries.GetAppointmentsByEstablishmentQuery;
import com.takodu.api.booking.domain.repositories.AppointmentRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AppointmentQueryServiceImpl implements AppointmentQueryService {
    private final AppointmentRepository appointmentRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public AppointmentQueryServiceImpl(AppointmentRepository appointmentRepository,
                                       ExternalOrganizationService externalOrganizationService, AccessPolicy accessPolicy) {
        this.appointmentRepository = appointmentRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
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
}
