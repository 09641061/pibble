package com.takodu.api.booking.infrastructure.persistence.jpa.converters;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text ({@code in_progress}), as the {@code CHECK} constraint expects. */
@Converter
public class AppointmentStatusPersistenceConverter implements AttributeConverter<AppointmentStatus, String> {
    @Override
    public String convertToDatabaseColumn(AppointmentStatus attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public AppointmentStatus convertToEntityAttribute(String column) { return column == null ? null : AppointmentStatus.valueOf(column.toUpperCase(Locale.ROOT)); }
}
