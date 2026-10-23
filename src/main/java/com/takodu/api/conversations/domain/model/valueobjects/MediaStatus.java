package com.takodu.api.conversations.domain.model.valueobjects;

/** Whether a message's media is on Takodu's disk yet. */
public enum MediaStatus {
    /** Waiting for the media job, possibly after a failed attempt. */
    PENDING,
    /** Downloaded; the inbox can show it. */
    READY,
    /** It could not be downloaded, and will not be tried again. */
    FAILED,
    /** C5: its customer was deleted; the retention job deletes the file next. */
    REMOVING,
    /** The retention job deleted the file, after the customer was deleted or the retention period ended. */
    REMOVED;

    /** @return whether the file is gone or about to be, so the inbox shows it as removed */
    public boolean isRemoved() {
        return this == REMOVING || this == REMOVED;
    }
}
