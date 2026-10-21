package com.takodu.api.iam.infrastructure.persistence.jpa.converters;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailPersistenceConverter implements AttributeConverter<Email, String> {
    @Override
    public String convertToDatabaseColumn(Email attribute) { return attribute == null ? null : attribute.value(); }

    @Override
    public Email convertToEntityAttribute(String column) { return column == null ? null : new Email(column); }
}
