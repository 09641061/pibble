package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.util.Optional;

public interface MessageRepository {

    @CrossTenant("The webhook skips a wamid it already stored (W4); wamids are unique across WhatsApp")
    boolean existsByWamid(Wamid wamid);

    @CrossTenant("A status webhook updates the message it reports on; the message carries its organization")
    Optional<Message> lockByWamid(Wamid wamid);

    Message save(Message message);
}
