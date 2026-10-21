package com.takodu.api.booking.application.internal.commandservices;

import com.takodu.api.booking.application.commandservices.TimeBlockCommandService;
import com.takodu.api.booking.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.booking.domain.exceptions.TimeBlockNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.commands.CreateTimeBlockCommand;
import com.takodu.api.booking.domain.model.commands.DeleteTimeBlockCommand;
import com.takodu.api.booking.domain.repositories.TimeBlockRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TimeBlockCommandServiceImpl implements TimeBlockCommandService {
    private final TimeBlockRepository timeBlockRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public TimeBlockCommandServiceImpl(TimeBlockRepository timeBlockRepository,
                                       ExternalOrganizationService externalOrganizationService,
                                       AccessPolicy accessPolicy) {
        this.timeBlockRepository = timeBlockRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    @Transactional
    public TimeBlock handle(CreateTimeBlockCommand command) {
        accessPolicy.require(actionFor(command.staffMemberId(), command.requestedBy()));
        var earlier = timeBlockRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        externalOrganizationService.fetchCalendar(command.organizationId(), command.establishmentId());
        accessPolicy.requireEstablishment(command.establishmentId());
        if (command.staffMemberId() != null)
            externalOrganizationService.requireMemberWorksAt(command.organizationId(), command.staffMemberId(),
                    command.establishmentId());
        return timeBlockRepository.save(TimeBlock.create(command.organizationId(), command.establishmentId(),
                command.staffMemberId(), command.range(), command.reason(), command.idempotencyKey()));
    }

    @Override
    @Transactional
    public void handle(DeleteTimeBlockCommand command) {
        var block = timeBlockRepository.findById(command.organizationId(), command.timeBlockId())
                .orElseThrow(() -> new TimeBlockNotFoundException(command.timeBlockId()));
        accessPolicy.require(actionFor(block.getStaffMemberId(), command.requestedBy()));
        accessPolicy.requireEstablishment(block.getEstablishmentId());
        timeBlockRepository.delete(block);
    }

    /** B15: a block of the caller's own time needs less than one of someone else's or of the whole establishment. */
    private static Action actionFor(MemberId blocked, MemberId requestedBy) {
        return requestedBy.equals(blocked) ? Action.MANAGE_OWN_TIME_BLOCKS : Action.MANAGE_ANY_CALENDAR;
    }
}
