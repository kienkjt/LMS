package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseStatusEnumConverter implements AttributeConverter<CourseStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(CourseStatusEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public CourseStatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CourseStatusEnum.fromValue(dbData);
    }
}