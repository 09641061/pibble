package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class LineModePersistenceConverter implements AttributeConverter<LineMode, String> {
    @Override
    public String convertToDatabaseColumn(LineMode attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public LineMode convertToEntityAttribute(String column) { return column == null ? null : LineMode.valueOf(column.toUpperCase(Locale.ROOT)); }
}
