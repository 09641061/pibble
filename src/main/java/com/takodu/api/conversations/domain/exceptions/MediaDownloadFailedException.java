package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** 07 §4: the file could not be downloaded from WhatsApp and will not be tried again. */
public class MediaDownloadFailedException extends BusinessRuleViolationException {
    public MediaDownloadFailedException(String reason) {
        super("MEDIA_DOWNLOAD_FAILED", "The file could not be downloaded from WhatsApp: " + reason);
    }
}
