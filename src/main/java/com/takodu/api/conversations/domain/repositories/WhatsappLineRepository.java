package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.util.List;
import java.util.Optional;

public interface WhatsappLineRepository {
    Optional<WhatsappLine> findById(OrganizationId organizationId, WhatsappLineId lineId);

    /** @return the establishment's connected line, else the one connected most recently */
    Optional<WhatsappLine> findLatestByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId);

    Optional<WhatsappLine> findByLastIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    @CrossTenant("The webhook finds the line a message arrived on, and connecting checks W1 across organizations; "
            + "the line carries its organization")
    Optional<WhatsappLine> findByPhoneNumberId(String phoneNumberId);

    @CrossTenant("The webhook disconnects the lines of a WhatsApp Business Account that Meta reports removed")
    List<WhatsappLine> findConnectedByWabaId(String wabaId);

    /**
     * @throws com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException when a concurrent
     *         connection took the establishment or the number first (W1)
     */
    WhatsappLine save(WhatsappLine line);
}
