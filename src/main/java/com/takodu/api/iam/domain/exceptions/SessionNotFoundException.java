package com.takodu.api.iam.domain.exceptions;

import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

public class SessionNotFoundException extends ResourceNotFoundException {
    public SessionNotFoundException(SessionId sessionId) { super("Session", sessionId.value()); }
}
