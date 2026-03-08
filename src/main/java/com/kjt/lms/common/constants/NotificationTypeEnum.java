package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    COURSE_APPROVED("1", "Khóa học được duyệt"),
    COURSE_REJECTED("2", "Khóa học bị từ chối"),
    NEW_ENROLLMENT("3", "Có học viên đăng ký mới"),
    NEW_REVIEW("4", "Có đánh giá mới"),
    PAYMENT_SUCCESS("5", "Thanh toán thành công"),
    PAYMENT_FAILED("6", "Thanh toán thất bại"),
    QUIZ_RESULT("7", "Kết quả bài kiểm tra"),
    CERTIFICATE_ISSUED("8", "Cấp chứng chỉ"),
    AI_RECOMMENDATION("9", "Gợi ý khóa học"),
    SYSTEM("10", "Thông báo hệ thống");

    private final String value;
    private final String description;

    public static NotificationTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (NotificationTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid NotificationType value: " + value);
    }
}