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

    @Value("${otp.expiration-seconds:600}")
    private long otpExpirationSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    private static final String OTP_PATTERN = "[0-9]{6}";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void generateAndSaveOtp(String email, String purpose) {

        String key = buildKey(email, purpose);

        // Generate OTP
        String otpCode = generateSecureOtp();

        OtpRedis otp = OtpRedis.builder()
                .id(key)
                .otpCode(otpCode)
                .failedAttempts(0)
                .expiration(otpExpirationSeconds)
                .build();

        otpRedisRepository.save(otp);

        log.info("OTP generated for email {} purpose {}", email, purpose);

        emailService.sendOtpEmail(email, otpCode, purpose);
    }

    @Override
    public void validateOtp(String email, String purpose, String otpCode) {

        if (!otpCode.matches(OTP_PATTERN)) {
            throw new BusinessException(messageProvider.getMessage("auth.verifyOtp.invalid"));
        }

        String key = buildKey(email, purpose);

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

        log.info("OTP verified for email {} purpose {}", email, purpose);
    }

    private String generateSecureOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private String buildKey(String email, String purpose) {
        return email + ":" + purpose;
    }
}