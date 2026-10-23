package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

/** An archived establishment keeps its history but takes no new settings, services or staff. */
public class EstablishmentArchivedException extends BusinessRuleViolationException {
    public EstablishmentArchivedException(EstablishmentId establishmentId) {
        super("ESTABLISHMENT_ARCHIVED", "Establishment is archived: " + establishmentId.value());
    }
}
