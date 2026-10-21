package com.takodu.api.organization.infrastructure.persistence.jpa.entities;

import com.takodu.api.organization.infrastructure.persistence.jpa.converters.RolePersistenceConverter;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.infrastructure.persistence.jpa.converters.EmailPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.converters.SecretHashPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@Setter
public class MemberPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id")
    private UUID userId;

    @Convert(converter = EmailPersistenceConverter.class)
    @Column(name = "email", nullable = false, updatable = false)
    private Email email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Convert(converter = RolePersistenceConverter.class)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "bookable", nullable = false)
    private boolean bookable;

    @Convert(converter = SecretHashPersistenceConverter.class)
    @Column(name = "invite_token_hash")
    private SecretHash inviteTokenHash;

    @Column(name = "invite_expires_at")
    private Instant inviteExpiresAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_establishments", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "establishment_id")
    private Set<UUID> establishmentIds = new HashSet<>();
}
