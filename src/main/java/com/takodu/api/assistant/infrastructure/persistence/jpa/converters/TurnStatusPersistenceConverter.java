package com.takodu.api.assistant.infrastructure.persistence.jpa.converters;

import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class TurnStatusPersistenceConverter implements AttributeConverter<TurnStatus, String> {
    @Override
    public String convertToDatabaseColumn(TurnStatus attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public TurnStatus convertToEntityAttribute(String column) { return column == null ? null : TurnStatus.valueOf(column.toUpperCase(Locale.ROOT)); }
}
