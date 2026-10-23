package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** B5, B6: the member or the customer already has an active appointment that overlaps. */
public class SlotUnavailableException extends ConflictException {
    public SlotUnavailableException(String message) { super("Slot", "unavailable", message); }
}
