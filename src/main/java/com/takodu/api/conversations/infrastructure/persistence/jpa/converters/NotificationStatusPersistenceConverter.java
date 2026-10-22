package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.NotificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class NotificationStatusPersistenceConverter implements AttributeConverter<NotificationStatus, String> {
    @Override
    public String convertToDatabaseColumn(NotificationStatus attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public NotificationStatus convertToEntityAttribute(String column) { return column == null ? null : NotificationStatus.valueOf(column.toUpperCase(Locale.ROOT)); }
}
