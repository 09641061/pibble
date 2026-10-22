package com.takodu.api.conversations.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

/**
 * What conversations needs to know about an establishment before it connects a line or sends a message.
 *
 * @param archived whether the establishment is archived (O4)
 * @param planActive whether the organization's plan is active now (O5, O6)
 */
public record EstablishmentState(EstablishmentId establishmentId, boolean archived, boolean planActive) { }
