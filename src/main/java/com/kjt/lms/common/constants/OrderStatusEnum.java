package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING("1", "Chờ thanh toán"),
    COMPLETED("2", "Đã thanh toán"),
    CANCELLED("3", "Đã hủy"),
    REFUNDED("4", "Đã hoàn tiền");

    private final String value;
    private final String description;

    public static OrderStatusEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OrderStatusEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus value: " + value);
    }
}