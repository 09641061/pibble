package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** O4: an archived establishment takes no new line. */
public class EstablishmentArchivedException extends BusinessRuleViolationException {
    public EstablishmentArchivedException() {
        super("ESTABLISHMENT_ARCHIVED", "The establishment is archived");
    }
}
