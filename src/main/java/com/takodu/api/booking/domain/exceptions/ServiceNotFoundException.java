package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

public class ServiceNotFoundException extends ResourceNotFoundException {
    public ServiceNotFoundException(ServiceId serviceId) { super("Service", serviceId.value()); }
}
