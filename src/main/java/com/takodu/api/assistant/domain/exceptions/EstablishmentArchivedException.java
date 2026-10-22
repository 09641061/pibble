package com.takodu.api.assistant.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** O4: an archived establishment answers no customer. */
public class EstablishmentArchivedException extends BusinessRuleViolationException {
    public EstablishmentArchivedException() {
        super("ESTABLISHMENT_ARCHIVED", "The establishment is archived");
    }
}
