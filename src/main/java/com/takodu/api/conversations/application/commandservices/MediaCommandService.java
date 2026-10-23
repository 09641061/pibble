package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.model.commands.DownloadNextMediaCommand;
import com.takodu.api.conversations.domain.model.commands.RemoveCustomerMediaCommand;
import com.takodu.api.conversations.domain.model.commands.RemoveNextMediaCommand;

public interface MediaCommandService {

    /**
     * 07 §4: downloads the media due first and records the result on its message: stored, or failed and tried again
     * later, at most 5 times. Runs without a caller, for the media job.
     *
     * @return whether a download was due; false when nothing is left to download now
     */
    boolean handle(DownloadNextMediaCommand command);

    /**
     * C5: marks the media of the deleted customer's messages for the retention job. Runs in the customer deletion's
     * transaction and touches no file.
     */
    void handle(RemoveCustomerMediaCommand command);

    /**
     * 07 §4: deletes the file of the media due first for removal and marks the message's media removed. Runs without a
     * caller, for the retention job.
     *
     * @return whether a file was due; false when nothing is left to remove
     */
    boolean handle(RemoveNextMediaCommand command);
}
