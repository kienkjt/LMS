package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LessonTypeEnum {

    VIDEO("1", "Bài học video"),
    DOCUMENT("2", "Tài liệu"),
    QUIZ("3", "Trắc nghiệm"),
    CODING("4", "Bài tập lập trình"),
    TEXT("5", "Nội dung văn bản");

    private final String value;
    private final String description;

    public static LessonTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (LessonTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid LessonType value: " + value);
    }

}