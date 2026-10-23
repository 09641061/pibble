package com.takodu.api.organization.application.internal.commandservices;

import com.takodu.api.organization.application.commandservices.PlanCommandService;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.organization.domain.model.commands.ChangePlanCommand;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.organization.domain.repositories.PlanChangeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.Objects;

@ApplicationScoped
public class PlanCommandServiceImpl implements PlanCommandService {
    private final OrganizationRepository organizationRepository;
    private final PlanChangeRepository planChangeRepository;
    private final Clock clock;

    public PlanCommandServiceImpl(OrganizationRepository organizationRepository,
                                  PlanChangeRepository planChangeRepository, Clock clock) {
        this.organizationRepository = organizationRepository;
        this.planChangeRepository = planChangeRepository;
        this.clock = clock;
    }

    /** The organization row lock serializes changes, so a repeated key finds the first change and extends once. */
    @Override
    @Transactional
    public PlanChange handle(ChangePlanCommand command) {
        var organization = organizationRepository.findByIdForUpdate(command.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
        var earlier = planChangeRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var now = clock.instant();
        var activeUntil = command.extendDays() != null
                ? organization.planEndExtendedBy(command.extendDays(), now)
                : Objects.requireNonNullElse(command.activeUntil(), organization.getPlanActiveUntil());
        var change = organization.changePlan(Objects.requireNonNullElse(command.plan(), organization.getPlan()),
                activeUntil, command.operatorId(), command.note(), command.idempotencyKey(), now);
        organizationRepository.save(organization);
        return planChangeRepository.add(change);
    }
}
