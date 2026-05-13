package com.kjt.lms.model.request.admin;

import com.kjt.lms.common.constants.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListOrderRequest {
    private String keyword; // Tìm kiếm theo mã đơn hàng hoặc tên học sinh
    private OrderStatusEnum status; // Lọc theo trạng thái
    private BigDecimal minAmount; // Giá trị tối thiểu
    private BigDecimal maxAmount; // Giá trị tối đa
    private LocalDateTime fromDate; // Từ ngày
    private LocalDateTime toDate; // Đến ngày
    private int page;
    private int size;
}

