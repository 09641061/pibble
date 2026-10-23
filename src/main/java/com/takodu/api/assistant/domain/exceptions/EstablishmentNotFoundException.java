package com.takodu.api.assistant.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

public class EstablishmentNotFoundException extends ResourceNotFoundException {
    public EstablishmentNotFoundException(EstablishmentId establishmentId) {
        super("Establishment", establishmentId.value());
    }
}
