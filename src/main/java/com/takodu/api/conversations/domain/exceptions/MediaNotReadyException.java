package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** 07 §4: the media job has not stored the file yet; asking again later finds it. */
public class MediaNotReadyException extends ConflictException {
    public MediaNotReadyException() {
        super("Media", "not ready", "The file is still being downloaded from WhatsApp; try again shortly");
    }
}
