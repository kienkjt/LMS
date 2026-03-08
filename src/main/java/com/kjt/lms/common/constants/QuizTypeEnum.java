package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuizTypeEnum {

    SINGLE_CHOICE("1", "Chọn một đáp án"),
    MULTIPLE_CHOICE("2", "Chọn nhiều đáp án"),
    TRUE_FALSE("3", "Đúng / Sai"),
    FILL_BLANK("4", "Điền vào chỗ trống");

    private final String value;
    private final String description;

    public static QuizTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (QuizTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid QuizType value: " + value);
    }
}