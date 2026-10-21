package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.commands.BookAppointmentCommand;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.interfaces.rest.resources.BookAppointmentResource;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.UUID;

public final class BookAppointmentCommandFromResourceAssembler {
    private BookAppointmentCommandFromResourceAssembler() { }

    /** @param createdBy the signed-in user, from {@code TenantContext} */
    public static BookAppointmentCommand toCommandFromResource(OrganizationId organizationId, UserId createdBy,
                                                               UUID idempotencyKey, BookAppointmentResource resource) {
        return new BookAppointmentCommand(organizationId, new CustomerId(resource.customerId()),
                new ServiceId(resource.serviceId()),
                resource.staffMemberId() == null ? null : new MemberId(resource.staffMemberId()), resource.start(),
                BookingSource.STAFF, createdBy, new IdempotencyKey(idempotencyKey));
    }
}
