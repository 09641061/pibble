package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.valueobjects.Slot;
import com.takodu.api.booking.interfaces.rest.resources.SlotResource;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

public final class SlotResourceFromEntityAssembler {
    private SlotResourceFromEntityAssembler() { }

    public static SlotResource toResourceFromEntity(Slot slot) {
        return new SlotResource(slot.range().start(), slot.range().end(),
                slot.freeMembers().stream().map(MemberId::value).toList());
    }
}
