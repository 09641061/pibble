package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.model.commands.DownloadNextMediaCommand;

public interface MediaCommandService {

    /**
     * 07 §4: downloads the media due first and records the result on its message: stored, or failed and tried again
     * later, at most 5 times. Runs without a caller, for the media job.
     *
     * @return whether a download was due; false when nothing is left to download now
     */
    boolean handle(DownloadNextMediaCommand command);
}
