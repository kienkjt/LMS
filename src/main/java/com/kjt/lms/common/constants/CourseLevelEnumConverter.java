package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseLevelEnumConverter implements AttributeConverter<CourseLevelEnum, String> {

    @Override
    public String convertToDatabaseColumn(CourseLevelEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public CourseLevelEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CourseLevelEnum.fromValue(dbData);
    }
}