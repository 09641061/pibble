package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.conversations.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.exceptions.WhatsappLineNotFoundException;
import com.takodu.api.conversations.domain.exceptions.WhatsappSignupFailedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.ConnectWhatsappLineCommand;
import com.takodu.api.conversations.domain.model.commands.DisconnectWhatsappLineCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

/** W1, W2: only the owner connects and disconnects lines. */
public interface WhatsappLineCommandService {

    /**
     * @return the connected line, or the one an earlier request with the same key connected
     * @throws AccessDeniedException when the caller is not the owner
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws PlanInactiveException when the plan is inactive (O6)
     * @throws EstablishmentArchivedException when the establishment is archived
     * @throws LineAlreadyConnectedException when the establishment has a connected line or the number is on another
     *         establishment's line (W1)
     * @throws WhatsappSignupFailedException when Meta refuses a signup step
     */
    WhatsappLine handle(ConnectWhatsappLineCommand command);

    /**
     * Keeps the line and its conversations; webhooks for it are ignored from now on.
     *
     * @throws AccessDeniedException when the caller is not the owner
     * @throws WhatsappLineNotFoundException when the establishment has no connected line
     */
    void handle(DisconnectWhatsappLineCommand command);
}
