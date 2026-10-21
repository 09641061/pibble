package com.takodu.api.customers.infrastructure.persistence.jpa.converters;

import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class DocumentTypePersistenceConverter implements AttributeConverter<IdentityDocument.Type, String> {
    @Override
    public String convertToDatabaseColumn(IdentityDocument.Type attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public IdentityDocument.Type convertToEntityAttribute(String column) { return column == null ? null : IdentityDocument.Type.valueOf(column.toUpperCase(Locale.ROOT)); }
}
