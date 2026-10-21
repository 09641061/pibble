package com.takodu.api.customers.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.customers.infrastructure.persistence.jpa.entities.CustomerPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class CustomerPersistenceAssembler {
    private CustomerPersistenceAssembler() { }

    public static Customer toDomainFromPersistence(CustomerPersistenceEntity entity) {
        var document = entity.getDocumentType() == null ? null
                : new IdentityDocument(entity.getDocumentType(), entity.getDocumentNumber());
        var key = entity.getIdempotencyKey() == null ? null : new IdempotencyKey(entity.getIdempotencyKey());
        return new Customer(new CustomerId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new PhoneNumber(entity.getPhoneE164()), entity.getName(), entity.getWhatsappProfileName(), document, entity.getNotes(),
                entity.getDeletedAt(), key);
    }

    public static CustomerPersistenceEntity toPersistenceFromDomain(Customer customer) {
        var entity = new CustomerPersistenceEntity();
        entity.setId(customer.getId() == null ? null : customer.getId().value());
        entity.setOrganizationId(customer.getOrganizationId().value());
        entity.setPhoneE164(customer.getPhone().e164());
        entity.setName(customer.getName());
        entity.setWhatsappProfileName(customer.getWhatsappProfileName());
        entity.setDocumentType(customer.getDocument() == null ? null : customer.getDocument().type());
        entity.setDocumentNumber(customer.getDocument() == null ? null : customer.getDocument().number());
        entity.setNotes(customer.getNotes());
        entity.setIdempotencyKey(customer.getIdempotencyKey() == null ? null : customer.getIdempotencyKey().value());
        entity.setDeletedAt(customer.getDeletedAt());
        return entity;
    }
}
