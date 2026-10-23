package com.takodu.api.conversations.domain.model.valueobjects;

/** Whether a message's media is on Takodu's disk yet. */
public enum MediaStatus {
    /** Waiting for the media job, possibly after a failed attempt. */
    PENDING,
    /** Downloaded; the inbox can show it. */
    READY,
    /** It could not be downloaded, and will not be tried again. */
    FAILED
}
