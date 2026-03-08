package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(
        autoApply = true
)
public class CommonStatusEnumConverter implements AttributeConverter<CommonStatusEnum, Integer> {
    public Integer convertToDatabaseColumn(CommonStatusEnum attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    public CommonStatusEnum convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : CommonStatusEnum.fromValue(dbData);
    }
}
