package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseStatusEnum {

    DRAFT("1", "Bản nháp"),
    PENDING_REVIEW("2", "Chờ duyệt"),
    PUBLISHED("3", "Đã xuất bản"),
    REJECTED("4", "Bị từ chối"),
    ARCHIVED("5", "Đã lưu trữ");

    private final String value;
    private final String description;

    public static CourseStatusEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CourseStatusEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CourseStatus value: " + value);
    }

}
