package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

/** The establishment has no line, or none connected when one is needed. */
public class WhatsappLineNotFoundException extends ResourceNotFoundException {
    public WhatsappLineNotFoundException(EstablishmentId establishmentId) {
        super("Whatsapp line", "establishment " + establishmentId.value());
    }
}
