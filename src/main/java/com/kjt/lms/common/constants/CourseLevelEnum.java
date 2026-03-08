package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseLevelEnum {
    BEGINNER("1", "Người mới bắt đầu"),
    INTERMEDIATE("2", "Trung cấp"),
    ADVANCED("3", "Nâng cao"),
    ALL_LEVEL("4", "Tất cả cấp độ");

    private final String value;
    private final String description;

    public static CourseLevelEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CourseLevelEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CourseLevel value: " + value);
    }

}
