package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuizTypeEnumConverter implements AttributeConverter<QuizTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(QuizTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public QuizTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return QuizTypeEnum.fromValue(dbData);
    }
}
