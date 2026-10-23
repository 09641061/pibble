package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class DeliveryStatusPersistenceConverter implements AttributeConverter<DeliveryStatus, String> {
    @Override
    public String convertToDatabaseColumn(DeliveryStatus attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public DeliveryStatus convertToEntityAttribute(String column) { return column == null ? null : DeliveryStatus.valueOf(column.toUpperCase(Locale.ROOT)); }
}
