package com.takodu.api.conversations.domain.model.valueobjects;

/** W2: how the number is connected to the Cloud API. */
public enum LineMode {
    /** The number stays in the owner's WhatsApp Business app; both sides see every message (default). */
    COEXISTENCE,
    /** The number lives only in the Cloud API, registered with a two-step verification PIN. */
    CLOUD_API
}
