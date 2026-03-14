package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CommonStatusEnumConverter;
import com.kjt.lms.common.constants.GenderEnum;
import com.kjt.lms.common.constants.GenderEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gender", length = 20)
    @Convert(converter = GenderEnumConverter.class)
    private GenderEnum gender;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_id", length = 200)
    private String providerId;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "ai_preferences", columnDefinition = "TEXT")
    private String aiPreferences;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "active", nullable = false)
    @Convert(converter = CommonStatusEnumConverter.class)
    private CommonStatusEnum active = CommonStatusEnum.ACTIVE;
}