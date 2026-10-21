package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.interfaces.rest.resources.MemberResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

import java.util.Locale;
import java.util.stream.Collectors;

public final class MemberResourceFromEntityAssembler {
    private MemberResourceFromEntityAssembler() { }

    public static MemberResource toResourceFromEntity(Member member) {
        return new MemberResource(member.getId().value(), member.getEmail().value(), member.getDisplayName(),
                member.getRole().name().toLowerCase(Locale.ROOT), member.isBookable(),
                member.isPending() ? "pending" : "active",
                member.getEstablishmentIds().stream().map(EstablishmentId::value).collect(Collectors.toSet()),
                member.getInviteExpiresAt(), member.getJoinedAt());
    }
}
