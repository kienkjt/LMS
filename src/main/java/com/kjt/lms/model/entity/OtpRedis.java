package com.kjt.lms.model.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Setter
@Builder
@RedisHash("otp")
public class OtpRedis {
    @Id
    private String id; // Có thể là email + purpose để đảm bảo uniqueness
    private String otpCode;
    private int failedAttempts;

    @TimeToLive
    private Long expiration; // Tính bằng giây
}