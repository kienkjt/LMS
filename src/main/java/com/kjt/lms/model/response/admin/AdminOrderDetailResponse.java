package com.kjt.lms.model.response.admin;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.response.order.OrderItemResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDetailResponse {
    private UUID id;
    private String orderCode; // Mã đơn hàng

    // Thông tin học sinh
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;

    // Thông tin đơn hàng
    private BigDecimal totalAmount; // Tổng tiền
    private BigDecimal discountAmount; // Tiền giảm giá
    private BigDecimal finalAmount; // Tiền cuối cùng
    private OrderStatusEnum status; // Trạng thái

    // Thanh toán
    private String paymentMethod;
    private String transactionId; // ID giao dịch từ VnPay
    private LocalDateTime createdAt; // Ngày tạo
    private LocalDateTime paidAt; // Ngày thanh toán
    private String note; // Ghi chú

    // Chi tiết items
    private List<OrderItemResponseDto> items;
}

