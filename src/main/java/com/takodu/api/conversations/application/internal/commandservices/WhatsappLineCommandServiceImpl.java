package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.WhatsappLineCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.OnboardedNumber;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappOnboarding;
import com.takodu.api.conversations.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.exceptions.WhatsappLineNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.ConnectWhatsappLineCommand;
import com.takodu.api.conversations.domain.model.commands.DisconnectWhatsappLineCommand;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.Optional;

/**
 * Connecting calls Meta while the owner waits, so it is not one {@code @Transactional} method: Meta's steps run
 * first, outside any transaction, and only storing the line runs in one (takodu-jobs).
 */
@ApplicationScoped
public class WhatsappLineCommandServiceImpl implements WhatsappLineCommandService {
    private final WhatsappLineRepository whatsappLineRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final WhatsappOnboarding whatsappOnboarding;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public WhatsappLineCommandServiceImpl(WhatsappLineRepository whatsappLineRepository,
                                          ExternalOrganizationService externalOrganizationService,
                                          WhatsappOnboarding whatsappOnboarding, AccessPolicy accessPolicy,
                                          Clock clock) {
        this.whatsappLineRepository = whatsappLineRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.whatsappOnboarding = whatsappOnboarding;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public WhatsappLine handle(ConnectWhatsappLineCommand command) {
        accessPolicy.require(Action.CONNECT_WHATSAPP);
        var earlier = whatsappLineRepository.findByLastIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var establishment = externalOrganizationService.fetchEstablishment(command.organizationId(),
                command.establishmentId());
        if (!establishment.planActive()) throw new PlanInactiveException(command.organizationId().value());
        if (establishment.archived()) throw new EstablishmentArchivedException();
        lineToReconnect(command);

        var onboarded = whatsappOnboarding.onboard(command.code(), command.wabaId(), command.phoneNumberId(),
                command.mode(), command.pin());
        return QuarkusTransaction.requiringNew().call(() -> store(command, onboarded));
    }

    @Override
    public void handle(DisconnectWhatsappLineCommand command) {
        accessPolicy.require(Action.CONNECT_WHATSAPP);
        externalOrganizationService.fetchEstablishment(command.organizationId(), command.establishmentId());
        var line = QuarkusTransaction.requiringNew().call(() -> {
            var connected = whatsappLineRepository.findLatestByEstablishment(command.organizationId(),
                            command.establishmentId())
                    .filter(WhatsappLine::isConnected)
                    .orElseThrow(() -> new WhatsappLineNotFoundException(command.establishmentId()));
            connected.disconnect();
            return whatsappLineRepository.save(connected);
        });
        try {
            whatsappOnboarding.unsubscribe(line);
        } catch (RuntimeException e) {
            Log.warnf(e, "Unsubscribing WhatsApp line %s failed; its webhooks are ignored", line.getId().value());
        }
    }

    private WhatsappLine store(ConnectWhatsappLineCommand command, OnboardedNumber onboarded) {
        var now = clock.instant();
        var line = lineToReconnect(command).map(disconnected -> {
            disconnected.reconnect(command.wabaId(), onboarded.displayPhone(), onboarded.accessToken(), command.mode(),
                    onboarded.templates(), now, command.idempotencyKey());
            return disconnected;
        }).orElseGet(() -> WhatsappLine.connect(command.organizationId(), command.establishmentId(), command.wabaId(),
                command.phoneNumberId(), onboarded.displayPhone(), onboarded.accessToken(), command.mode(),
                onboarded.templates(), now, command.idempotencyKey()));
        return whatsappLineRepository.save(line);
    }

    /**
     * W1: checked before Meta is called, so a refused connection spends no signup code, and again before storing.
     *
     * @return the establishment's disconnected line with the same number, to reuse
     * @throws LineAlreadyConnectedException when the establishment has a connected line or the number belongs to
     *         another establishment
     */
    private Optional<WhatsappLine> lineToReconnect(ConnectWhatsappLineCommand command) {
        var current = whatsappLineRepository.findLatestByEstablishment(command.organizationId(), command.establishmentId())
                .filter(WhatsappLine::isConnected);
        if (current.isPresent()) {
            throw new LineAlreadyConnectedException(current.get().getPhoneNumberId().equals(command.phoneNumberId())
                    ? "This number is already connected to the establishment"
                    : "The establishment already has a connected line; disconnect it first");
        }
        var sameNumber = whatsappLineRepository.findByPhoneNumberId(command.phoneNumberId());
        if (sameNumber.isPresent() && !sameNumber.get().getEstablishmentId().equals(command.establishmentId()))
            throw new LineAlreadyConnectedException("This number is connected to another establishment");
        return sameNumber;
    }
}
