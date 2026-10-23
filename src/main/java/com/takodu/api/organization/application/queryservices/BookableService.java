package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Service;

import java.util.List;

/**
 * A service together with the members who can perform it now (O15).
 *
 * @param performers active, bookable members working at the service's establishment, in {@link Member#TEAM_ORDER}
 */
public record BookableService(Service service, List<Member> performers) { }
