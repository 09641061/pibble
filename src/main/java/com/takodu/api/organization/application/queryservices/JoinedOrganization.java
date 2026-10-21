package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Organization;

/**
 * An organization together with the caller's membership in it.
 */
public record JoinedOrganization(Organization organization, Member membership) { }
