package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "instructor_wallets")
public class InstructorWalletEntity extends BaseEntity {

    @Column(name = "instructor_id", nullable = false, length = 36, unique = true)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID instructorId;

    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Column(name = "total_commission_deducted", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCommissionDeducted = BigDecimal.ZERO;

    @Column(name = "total_refunded", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRefunded = BigDecimal.ZERO;

    @Version
    private Long version;
}

