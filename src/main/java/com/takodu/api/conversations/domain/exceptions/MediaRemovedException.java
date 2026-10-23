package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

/** C5 and 07 §4: the file was deleted with its customer or after the retention period; the message stays. */
public class MediaRemovedException extends ResourceNotFoundException {
    public MediaRemovedException() {
        super("Media", "removed", "The file was deleted: its customer was deleted or it passed the retention period");
    }
}
