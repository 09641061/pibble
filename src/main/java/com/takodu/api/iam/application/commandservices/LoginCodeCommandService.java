package com.takodu.api.iam.application.commandservices;

import com.takodu.api.iam.domain.exceptions.LoginCodeLimitReachedException;
import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.commands.RequestLoginCodeCommand;

public interface LoginCodeCommandService {

    /**
     * Stores a new code and emails it. The email is sent after the code is committed, outside any transaction.
     *
     * @param command the address to send the code to
     * @return the stored code
     * @throws LoginCodeLimitReachedException when the email already received five codes in the last hour (I3)
     */
    LoginCode handle(RequestLoginCodeCommand command);
}
