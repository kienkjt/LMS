package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.constants.OrderStatusEnumConverter;
import com.kjt.lms.common.constants.PaymentMethodEnum;
import com.kjt.lms.common.constants.PaymentMethodEnumConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID studentId;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "status", nullable = false)
    @Convert(converter = OrderStatusEnumConverter.class)
    private OrderStatusEnum status;

    @Column(name = "payment_method", nullable = false)
    @Convert(converter = PaymentMethodEnumConverter.class)
    private PaymentMethodEnum paymentMethod;

    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}