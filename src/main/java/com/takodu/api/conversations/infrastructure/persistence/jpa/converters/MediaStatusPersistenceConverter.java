package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.MediaStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class MediaStatusPersistenceConverter implements AttributeConverter<MediaStatus, String> {
    @Override
    public String convertToDatabaseColumn(MediaStatus attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public MediaStatus convertToEntityAttribute(String column) { return column == null ? null : MediaStatus.valueOf(column.toUpperCase(Locale.ROOT)); }
}
