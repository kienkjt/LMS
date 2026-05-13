package com.kjt.lms.model.request.admin;

import com.kjt.lms.common.constants.OrderStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull(message = "Status không được để trống")
    private OrderStatusEnum status;

    private String reason; // Lý do cập nhật (đặc biệt cho hủy/hoàn tiền)
}

