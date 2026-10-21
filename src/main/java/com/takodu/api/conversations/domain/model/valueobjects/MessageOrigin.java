package com.takodu.api.conversations.domain.model.valueobjects;

/** Who wrote a message. */
public enum MessageOrigin {
    CUSTOMER,
    ASSISTANT,
    STAFF,
    /** W5: the owner, from the WhatsApp Business app on their phone. */
    BUSINESS_APP,
    /** A template Takodu sends on its own, such as a reminder. */
    SYSTEM,
    /** Imported from the phone's chat history when the line was connected. */
    HISTORY
}
