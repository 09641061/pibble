package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.domain.exceptions.LineDisconnectedException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.aggregates.ScheduledMessage;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.CancelAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.DispatchNextDueNotificationCommand;
import com.takodu.api.conversations.domain.model.commands.MoveAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.commands.ScheduleAppointmentNotificationsCommand;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.OutboundMessageQueuedEvent;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.domain.repositories.ScheduledMessageRepository;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ScheduledMessageCommandServiceImpl implements ScheduledMessageCommandService {
    private static final String TEMPLATE_NOT_APPROVED = "The template is not approved by Meta";
    private final ScheduledMessageRepository scheduledMessageRepository;
    private final WhatsappLineRepository whatsappLineRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalCustomersService externalCustomersService;
    private final Event<OutboundMessageQueuedEvent> outboundMessageQueued;
    private final Event<ConversationChangedEvent> conversationChanged;
    private final Clock clock;

    public ScheduledMessageCommandServiceImpl(ScheduledMessageRepository scheduledMessageRepository,
                                              WhatsappLineRepository whatsappLineRepository,
                                              ConversationRepository conversationRepository,
                                              MessageRepository messageRepository,
                                              ExternalOrganizationService externalOrganizationService,
                                              ExternalCustomersService externalCustomersService,
                                              Event<OutboundMessageQueuedEvent> outboundMessageQueued,
                                              Event<ConversationChangedEvent> conversationChanged, Clock clock) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.whatsappLineRepository = whatsappLineRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalCustomersService = externalCustomersService;
        this.outboundMessageQueued = outboundMessageQueued;
        this.conversationChanged = conversationChanged;
        this.clock = clock;
    }

    /** W11, W12. A repeated event finds the rows and adds nothing. */
    @Override
    @Transactional
    public void handle(ScheduleAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        var now = clock.instant();
        if (kindOf(existing, NotificationKind.REMINDER).isEmpty()) {
            ScheduledMessage.reminderFor(command.organizationId(), command.establishmentId(), command.appointmentId(),
                    command.customerId(), command.serviceId(), command.startsAt(), now)
                    .ifPresent(scheduledMessageRepository::save);
        }
        if (command.bookedByStaff() && kindOf(existing, NotificationKind.CONFIRMATION).isEmpty()) {
            scheduledMessageRepository.save(ScheduledMessage.confirmationFor(command.organizationId(),
                    command.establishmentId(), command.appointmentId(), command.customerId(), command.serviceId(),
                    command.startsAt(), now));
        }
    }

    /** W13, decision 62. An appointment booked too close for a reminder gets one when it moves far enough. */
    @Override
    @Transactional
    public void handle(MoveAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        var now = clock.instant();
        for (var notification : existing) {
            notification.reschedule(command.startsAt(), now);
            scheduledMessageRepository.save(notification);
        }
        if (kindOf(existing, NotificationKind.REMINDER).isEmpty()) {
            ScheduledMessage.reminderFor(command.organizationId(), command.establishmentId(), command.appointmentId(),
                    command.customerId(), command.serviceId(), command.startsAt(), now)
                    .ifPresent(scheduledMessageRepository::save);
        }
    }

    /** W13, decision 63. */
    @Override
    @Transactional
    public void handle(CancelAppointmentNotificationsCommand command) {
        var existing = scheduledMessageRepository.lockByAppointment(command.organizationId(), command.appointmentId());
        for (var notification : existing) {
            notification.cancel("The appointment was cancelled");
            scheduledMessageRepository.save(notification);
        }
        var now = clock.instant();
        if (command.cancelledByStaff() && command.startsAt().isAfter(now)
                && kindOf(existing, NotificationKind.CANCELLATION).isEmpty()) {
            scheduledMessageRepository.save(ScheduledMessage.cancellationFor(command.organizationId(),
                    command.establishmentId(), command.appointmentId(), command.customerId(), command.serviceId(),
                    command.startsAt(), command.reason(), now));
        }
    }

    /**
     * One row per transaction, locked until it is queued, held or skipped (takodu-jobs). It does not call Meta: the
     * message it queues goes through the outbox like any other, with its retries and failed state (W14).
     */
    @Override
    @Transactional
    public boolean handle(DispatchNextDueNotificationCommand command) {
        var claimed = scheduledMessageRepository.claimNextDue(command.now());
        if (claimed.isEmpty()) return false;
        var notification = claimed.get();
        dispatch(notification, command.now());
        scheduledMessageRepository.save(notification);
        return true;
    }

    /** W12, W13, W15 and decision 65, checked in this order. */
    private void dispatch(ScheduledMessage notification, Instant now) {
        var lateness = notification.lateness(now);
        if (lateness.isPresent()) {
            notification.skip(lateness.get());
            return;
        }
        var organizationId = notification.getOrganizationId();
        var establishment = externalOrganizationService.fetchEstablishment(organizationId,
                notification.getEstablishmentId());
        if (!establishment.planActive()) {
            notification.hold("The plan is inactive", now);
            return;
        }
        var line = whatsappLineRepository.findLatestByEstablishment(organizationId, notification.getEstablishmentId())
                .filter(WhatsappLine::isConnected);
        if (line.isEmpty()) {
            notification.hold(LineDisconnectedException.MESSAGE, now);
            return;
        }
        var template = notification.getKind().template();
        if (notification.getKind() == NotificationKind.REMINDER && !line.get().canSend(template)) {
            notification.hold(TEMPLATE_NOT_APPROVED, now);
            return;
        }
        var customer = externalCustomersService.fetchContact(organizationId, notification.getCustomerId())
                .filter(contact -> !contact.deleted());
        var serviceName = externalOrganizationService.fetchServiceName(organizationId, notification.getServiceId());
        if (customer.isEmpty() || serviceName.isEmpty()) {
            notification.skip("The customer or the service no longer exists");
            return;
        }
        var conversation = conversationRepository.lockOrStart(line.get(), notification.getCustomerId(), now);
        var asTemplate = notification.getKind() == NotificationKind.REMINDER || !conversation.isWindowOpen(now);
        if (asTemplate && !line.get().canSend(template)) {
            notification.hold(TEMPLATE_NOT_APPROVED, now);
            return;
        }
        var parameters = notification.parameters(customer.get().displayName(), serviceName.get(), establishment.name(),
                establishment.timeZone());
        var message = messageRepository.save(Message.notification(conversation, template, parameters, asTemplate, now));
        conversation.recordNotification(now);
        conversationRepository.save(conversation);
        notification.markSent(message.getId());
        outboundMessageQueued.fire(new OutboundMessageQueuedEvent(message.getId()));
        conversationChanged.fire(new ConversationChangedEvent(conversation.getOrganizationId(),
                conversation.getEstablishmentId(), conversation.getId()));
    }

    private static Optional<ScheduledMessage> kindOf(List<ScheduledMessage> notifications, NotificationKind kind) {
        return notifications.stream().filter(notification -> notification.getKind() == kind).findFirst();
    }
}
