package com.takodu.api.shared.infrastructure.persistence.jpa.converters;

import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SecretHashPersistenceConverter implements AttributeConverter<SecretHash, String> {
    @Override
    public String convertToDatabaseColumn(SecretHash attribute) { return attribute == null ? null : attribute.value(); }

    @Override
    public SecretHash convertToEntityAttribute(String column) { return column == null ? null : new SecretHash(column); }
}
