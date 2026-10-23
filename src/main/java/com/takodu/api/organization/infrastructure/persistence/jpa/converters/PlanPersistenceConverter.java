package com.takodu.api.organization.infrastructure.persistence.jpa.converters;

import com.takodu.api.organization.domain.model.valueobjects.Plan;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class PlanPersistenceConverter implements AttributeConverter<Plan, String> {
    @Override
    public String convertToDatabaseColumn(Plan attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public Plan convertToEntityAttribute(String column) { return column == null ? null : Plan.valueOf(column.toUpperCase(Locale.ROOT)); }
}
