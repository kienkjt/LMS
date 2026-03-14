package com.kjt.lms.common.constants;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LessonTypeEnumConverter extends GenericEnumConverter<LessonTypeEnum> {

    public LessonTypeEnumConverter() {
        super(LessonTypeEnum.class);
    }
}