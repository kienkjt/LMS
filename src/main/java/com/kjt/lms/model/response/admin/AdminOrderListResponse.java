package com.kjt.lms.model.response.admin;

import com.kjt.lms.common.constants.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListResponse {
    private UUID id;
    private String orderCode; // Mã đơn hàng
    private UUID studentId; // ID học sinh
    private String studentName; // Tên học sinh
    private String studentEmail; // Email học sinh
    private BigDecimal totalAmount; // Tổng tiền
    private BigDecimal discountAmount; // Tiền giảm giá
    private BigDecimal finalAmount; // Tiền cuối cùng
    private OrderStatusEnum status; // Trạng thái
    private String paymentMethod; // Phương thức thanh toán
    private LocalDateTime createdAt; // Ngày tạo
    private LocalDateTime paidAt; // Ngày thanh toán
    private int itemCount; // Số lượng mặt hàng trong đơn hàng
}

