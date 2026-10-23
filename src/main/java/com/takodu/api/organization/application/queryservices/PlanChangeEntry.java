package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.shared.domain.model.valueobjects.Email;

/**
 * A plan change with the email of the operator who made it.
 *
 * @param operatorEmail empty only if the user no longer exists
 */
public record PlanChangeEntry(PlanChange change, Email operatorEmail) { }
