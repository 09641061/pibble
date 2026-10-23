package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.shared.domain.model.valueobjects.Email;

/**
 * An organization as the Takodu team sees it when following payments.
 *
 * @param ownerEmail who to contact about the plan
 * @param activeEstablishments compared with the plan's limit (O4)
 * @param planActive whether the plan is active now (O5)
 */
public record OrganizationAccount(Organization organization, Email ownerEmail, int activeEstablishments,
                                  boolean planActive) { }
