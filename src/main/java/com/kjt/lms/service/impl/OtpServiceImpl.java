package com.kjt.lms.service.impl;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.OtpRedis;
import com.kjt.lms.repository.OtpRedisRepository;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpRedisRepository otpRedisRepository;
    private final MessageProvider messageProvider;
    private final EmailService emailService;

    @Value("${otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.reset-verified-ttl-seconds:300}")
    private long resetVerifiedTtlSeconds;

    @Value("${otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    private static final String OTP_PATTERN = "[0-9]{6}";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RESET_VERIFIED_PREFIX = "PASSWORD_RESET_VERIFIED:";
    private static final String RESEND_COOLDOWN_PREFIX = "OTP_RESEND_COOLDOWN:";

    @Override
    public void generateAndSaveOtp(String email, String purpose) {

        String normalizedEmail = normalizeEmail(email);
        String key = buildKey(normalizedEmail, purpose);
        String cooldownKey = resendCooldownKey(normalizedEmail, purpose);

        if (otpRedisRepository.existsById(cooldownKey)) {
            throw new BusinessException(messageProvider.getMessage("otp.rate_limit_exceeded"));
        }

        // Generate OTP
        String otpCode = generateSecureOtp();

        OtpRedis otp = OtpRedis.builder()
                .id(key)
                .otpCode(otpCode)
                .failedAttempts(0)
                .expiration(otpExpirationMinutes * 60)
                .build();

        otpRedisRepository.save(otp);
        otpRedisRepository.save(OtpRedis.builder()
                .id(cooldownKey)
                .otpCode("COOLDOWN")
                .failedAttempts(0)
                .expiration(resendCooldownSeconds)
                .build());

        log.info("OTP generated for email {} purpose {}", normalizedEmail, purpose);

        emailService.sendOtpEmail(normalizedEmail, otpCode, purpose);
    }

    @Override
    public void validateOtp(String email, String purpose, String otpCode) {

        if (!otpCode.matches(OTP_PATTERN)) {
            throw new BusinessException(messageProvider.getMessage("auth.verifyOtp.invalid"));
        }

        String normalizedEmail = normalizeEmail(email);
        String key = buildKey(normalizedEmail, purpose);

        Optional<OtpRedis> optionalOtp = otpRedisRepository.findById(key);

        if (optionalOtp.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("otp.expired"));
        }

        OtpRedis otp = optionalOtp.get();

        // Check max attempts
        if (otp.getFailedAttempts() >= maxAttempts) {
            otpRedisRepository.deleteById(key);
            throw new BusinessException(messageProvider.getMessage("otp.max_attempts_exceeded"));
        }

        // Validate OTP
        if (!otp.getOtpCode().equals(otpCode)) {

            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otpRedisRepository.save(otp);

            int remainingAttempts = maxAttempts - otp.getFailedAttempts();

            if (remainingAttempts <= 0) {
                otpRedisRepository.deleteById(key);
                throw new BusinessException(messageProvider.getMessage("otp.max_attempts_exceeded"));
            }

            throw new BusinessException(messageProvider.getMessage("otp.invalid", remainingAttempts));
        }

        // Success -> delete OTP
        otpRedisRepository.deleteById(key);

        otpRedisRepository.deleteById(resendCooldownKey(normalizedEmail, purpose));

        log.info("OTP verified for email {} purpose {}", normalizedEmail, purpose);
    }

    @Override
    public void markPasswordResetVerified(String email) {
        OtpRedis resetVerified = OtpRedis.builder()
                .id(resetVerifiedKey(normalizeEmail(email)))
                .otpCode("VERIFIED")
                .failedAttempts(0)
                .expiration(resetVerifiedTtlSeconds)
                .build();

        otpRedisRepository.save(resetVerified);
    }

    @Override
    public void requirePasswordResetVerified(String email) {
        OtpRedis verified = otpRedisRepository.findById(resetVerifiedKey(normalizeEmail(email)))
                .orElseThrow(() -> new BusinessException(messageProvider.getMessage("auth.resetPassword.otpNotVerified")));

        if (!"VERIFIED".equals(verified.getOtpCode())) {
            throw new BusinessException(messageProvider.getMessage("auth.resetPassword.otpNotVerified"));
        }
    }

    @Override
    public void clearPasswordResetVerified(String email) {
        otpRedisRepository.deleteById(resetVerifiedKey(normalizeEmail(email)));
    }

    private String generateSecureOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private String buildKey(String email, String purpose) {
        return email + ":" + purpose;
    }

    private String resetVerifiedKey(String email) {
        return RESET_VERIFIED_PREFIX + email;
    }

    private String resendCooldownKey(String email, String purpose) {
        return RESEND_COOLDOWN_PREFIX + buildKey(email, purpose);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
