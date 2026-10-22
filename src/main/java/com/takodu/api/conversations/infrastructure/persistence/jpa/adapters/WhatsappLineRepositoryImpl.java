package com.takodu.api.conversations.infrastructure.persistence.jpa.adapters;

import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers.WhatsappLinePersistenceAssembler;
import com.takodu.api.conversations.infrastructure.persistence.jpa.repositories.WhatsappLinePersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WhatsappLineRepositoryImpl implements WhatsappLineRepository {
    private static final String PHONE_NUMBER_UNIQUE = "whatsapp_lines_phone_number_uk";
    private static final String ESTABLISHMENT_UNIQUE = "whatsapp_lines_establishment_uk";

    private final WhatsappLinePersistenceRepository whatsappLinePersistenceRepository;

    public WhatsappLineRepositoryImpl(WhatsappLinePersistenceRepository whatsappLinePersistenceRepository) {
        this.whatsappLinePersistenceRepository = whatsappLinePersistenceRepository;
    }

    @Override
    public Optional<WhatsappLine> findById(OrganizationId organizationId, WhatsappLineId lineId) {
        return whatsappLinePersistenceRepository.findById(organizationId.value(), lineId.value())
                .map(WhatsappLinePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<WhatsappLine> findLatestByEstablishment(OrganizationId organizationId,
                                                            EstablishmentId establishmentId) {
        return whatsappLinePersistenceRepository.findLatestByEstablishment(organizationId.value(),
                establishmentId.value()).map(WhatsappLinePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<WhatsappLine> findByLastIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return whatsappLinePersistenceRepository.findByLastIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(WhatsappLinePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<WhatsappLine> findByPhoneNumberId(String phoneNumberId) {
        return whatsappLinePersistenceRepository.findByPhoneNumberId(phoneNumberId)
                .map(WhatsappLinePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<WhatsappLine> findConnectedByWabaId(String wabaId) {
        return whatsappLinePersistenceRepository.findConnectedByWabaId(wabaId).stream()
                .map(WhatsappLinePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public WhatsappLine save(WhatsappLine line) {
        var entity = WhatsappLinePersistenceAssembler.toPersistenceFromDomain(line);
        try {
            if (entity.getId() == null) {
                whatsappLinePersistenceRepository.persist(entity);
            } else {
                entity = whatsappLinePersistenceRepository.getEntityManager().merge(entity);
            }
            whatsappLinePersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case PHONE_NUMBER_UNIQUE -> throw new LineAlreadyConnectedException("This number is connected to another establishment");
                case ESTABLISHMENT_UNIQUE -> throw new LineAlreadyConnectedException("The establishment already has a connected line");
                case null, default -> throw e;
            }
        }
        return WhatsappLinePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
