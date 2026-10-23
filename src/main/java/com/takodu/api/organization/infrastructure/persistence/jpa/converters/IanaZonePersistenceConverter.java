package com.takodu.api.organization.infrastructure.persistence.jpa.converters;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IanaZonePersistenceConverter implements AttributeConverter<IanaZone, String> {
    @Override
    public String convertToDatabaseColumn(IanaZone attribute) { return attribute == null ? null : attribute.value().getId(); }

    @Override
    public IanaZone convertToEntityAttribute(String column) { return column == null ? null : IanaZone.of(column); }
}
