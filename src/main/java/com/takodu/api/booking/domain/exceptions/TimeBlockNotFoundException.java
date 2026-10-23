package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

public class TimeBlockNotFoundException extends ResourceNotFoundException {
    public TimeBlockNotFoundException(TimeBlockId timeBlockId) { super("Time block", timeBlockId.value()); }
}
