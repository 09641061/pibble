package com.takodu.api.organization.application.internal.commandservices;

import com.takodu.api.organization.application.commandservices.MemberCommandService;
import com.takodu.api.organization.application.internal.outboundservices.acl.ExternalIamService;
import com.takodu.api.organization.application.internal.outboundservices.email.InvitationSender;
import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.InvitationNotFoundException;
import com.takodu.api.organization.domain.exceptions.MemberAlreadyExistsException;
import com.takodu.api.organization.domain.exceptions.MemberNotFoundException;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.commands.AcceptInvitationCommand;
import com.takodu.api.organization.domain.model.commands.InviteMemberCommand;
import com.takodu.api.organization.domain.model.commands.RemoveMemberCommand;
import com.takodu.api.organization.domain.model.commands.RenewInvitationCommand;
import com.takodu.api.organization.domain.model.commands.TransferOwnershipCommand;
import com.takodu.api.organization.domain.model.commands.RenameOwnMembershipCommand;
import com.takodu.api.organization.domain.model.commands.UpdateMemberCommand;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.organization.domain.repositories.ServiceRepository;
import com.takodu.api.organization.interfaces.acl.UpcomingAppointmentsProvider;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MemberCommandServiceImpl implements MemberCommandService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final EstablishmentRepository establishmentRepository;
    private final ServiceRepository serviceRepository;
    private final ExternalIamService externalIamService;
    private final InvitationSender invitationSender;
    private final UpcomingAppointmentsProvider upcomingAppointmentsProvider;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public MemberCommandServiceImpl(MemberRepository memberRepository, OrganizationRepository organizationRepository,
                                    EstablishmentRepository establishmentRepository, ServiceRepository serviceRepository,
                                    ExternalIamService externalIamService, InvitationSender invitationSender,
                                    UpcomingAppointmentsProvider upcomingAppointmentsProvider,
                                    AccessPolicy accessPolicy, Clock clock) {
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.establishmentRepository = establishmentRepository;
        this.serviceRepository = serviceRepository;
        this.externalIamService = externalIamService;
        this.invitationSender = invitationSender;
        this.upcomingAppointmentsProvider = upcomingAppointmentsProvider;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public Member handle(InviteMemberCommand command) {
        var token = newToken();
        var invitation = QuarkusTransaction.requiringNew().call(() -> invite(command, SecretHash.of(token)));
        if (invitation.isNew()) send(invitation, token);
        return invitation.member();
    }

    @Override
    @Transactional
    public JoinedOrganization handle(AcceptInvitationCommand command) {
        var member = memberRepository.findByInviteTokenForUpdate(SecretHash.of(command.token()))
                .orElseThrow(InvitationNotFoundException::new);
        var email = externalIamService.emailOf(command.userId())
                .orElseThrow(() -> new IllegalStateException("The signed-in user does not exist"));
        member.accept(command.userId(), email, clock.instant());
        var organization = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(member.getOrganizationId()));
        return new JoinedOrganization(organization, memberRepository.save(member));
    }

    @Override
    public Member handle(RenewInvitationCommand command) {
        var token = newToken();
        var invitation = QuarkusTransaction.requiringNew().call(() -> renew(command, SecretHash.of(token)));
        if (invitation.isNew()) send(invitation, token);
        return invitation.member();
    }

    @Override
    @Transactional
    public Member handle(UpdateMemberCommand command) {
        accessPolicy.require(Action.MANAGE_MEMBERS);
        var member = load(command.organizationId(), command.memberId());
        if (member.getRole() != Role.OWNER || command.role() != Role.OWNER) {
            requireActiveEstablishments(command.organizationId(), command.establishmentIds());
            member.assign(command.role(), command.establishmentIds());
        }
        member.describe(command.displayName(), command.bookable());
        dropFromServicesTheyCannotPerform(member);
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public Member handle(RemoveMemberCommand command) {
        accessPolicy.require(Action.MANAGE_MEMBERS);
        var member = load(command.organizationId(), command.memberId());
        var now = clock.instant();
        member.remove(now, upcomingAppointmentsProvider.hasUpcomingAppointments(
                command.organizationId().value(), command.memberId().value(), now));
        dropFromServicesTheyCannotPerform(member);
        return memberRepository.save(member);
    }

    /**
     * The organization row stays locked until commit, so two transfers serialize. The retry check comes before the
     * role check because a retried request comes from the former owner, who is a manager by then.
     */
    @Override
    @Transactional
    public Member handle(TransferOwnershipCommand command) {
        var organization = organizationRepository.findByIdForUpdate(command.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
        var owner = memberRepository.findOwner(command.organizationId())
                .orElseThrow(() -> new IllegalStateException("Organization without owner: " + command.organizationId()));
        if (organization.alreadyApplied(command.idempotencyKey())) return owner;
        accessPolicy.require(Action.MANAGE_ORGANIZATION);
        if (!owner.getId().equals(command.callerId())) throw new AccessDeniedException("transfer ownership");
        var newOwner = load(command.organizationId(), command.newOwnerId());
        if (newOwner.getId().equals(owner.getId())) return owner;
        newOwner.promoteToOwner();
        owner.handOverOwnership();
        memberRepository.save(owner);
        var saved = memberRepository.save(newOwner);
        organization.recordTransfer(command.idempotencyKey());
        organizationRepository.save(organization);
        return saved;
    }

    /** O15 keeps holding after the member changed: removed, not bookable, or moved to other establishments. */
    private void dropFromServicesTheyCannotPerform(Member member) {
        for (var service : serviceRepository.findByPerformer(member.getOrganizationId(), member.getId()))
            if (service.dropIfIneligible(member)) serviceRepository.save(service);
    }

    @Override
    @Transactional
    public Member handle(RenameOwnMembershipCommand command) {
        var member = load(command.organizationId(), command.memberId());
        member.rename(command.displayName());
        return memberRepository.save(member);
    }

    private Member load(OrganizationId organizationId, MemberId memberId) {
        return memberRepository.findCurrentById(organizationId, memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    private Invitation invite(InviteMemberCommand command, SecretHash tokenHash) {
        accessPolicy.require(Action.MANAGE_MEMBERS);
        var earlier = memberRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return new Invitation(earlier.get(), false, null, null);
        if (memberRepository.existsCurrentByEmail(command.organizationId(), command.email()))
            throw new MemberAlreadyExistsException(command.email());
        requireActiveEstablishments(command.organizationId(), command.establishmentIds());
        var member = memberRepository.save(Member.invite(command.organizationId(), command.email(),
                command.displayName(), command.role(), command.establishmentIds(), command.bookable(), tokenHash,
                clock.instant(), command.idempotencyKey()));
        return invitationFrom(member, command.inviterId(), true);
    }

    private Invitation renew(RenewInvitationCommand command, SecretHash tokenHash) {
        accessPolicy.require(Action.MANAGE_MEMBERS);
        var member = load(command.organizationId(), command.memberId());
        if (member.alreadyApplied(command.idempotencyKey())) return new Invitation(member, false, null, null);
        member.renewInvitation(tokenHash, clock.instant(), command.idempotencyKey());
        return invitationFrom(memberRepository.save(member), command.inviterId(), true);
    }

    private Invitation invitationFrom(Member member, MemberId inviterId, boolean isNew) {
        var organization = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(member.getOrganizationId()));
        var inviter = memberRepository.findCurrentById(member.getOrganizationId(), inviterId)
                .map(Member::getDisplayName).orElse(organization.getName());
        return new Invitation(member, isNew, organization.getName(), inviter);
    }

    private void requireActiveEstablishments(OrganizationId organizationId, Set<EstablishmentId> establishmentIds) {
        for (var establishmentId : establishmentIds) {
            establishmentRepository.findById(organizationId, establishmentId)
                    .filter(Establishment::isActive)
                    .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId));
        }
    }

    private void send(Invitation invitation, String token) {
        var tokenHash = invitation.member().getInviteTokenHash().value();
        invitationSender.sendInvitation(invitation.member().getEmail(), invitation.organizationName(),
                invitation.invitedBy(), token, UUID.nameUUIDFromBytes(tokenHash.getBytes(StandardCharsets.US_ASCII)));
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** @param isNew whether this request created or renewed the invitation, so its email must be sent */
    private record Invitation(Member member, boolean isNew, String organizationName, String invitedBy) { }
}
