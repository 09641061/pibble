package com.takodu.api.booking.infrastructure.persistence.jpa.converters;

import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class CancelledByPersistenceConverter implements AttributeConverter<CancelledBy, String> {
    @Override
    public String convertToDatabaseColumn(CancelledBy attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public CancelledBy convertToEntityAttribute(String column) { return column == null ? null : CancelledBy.valueOf(column.toUpperCase(Locale.ROOT)); }
}
