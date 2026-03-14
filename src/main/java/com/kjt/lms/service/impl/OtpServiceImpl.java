package com.kjt.lms.service.impl;

import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.OtpEntity;
import com.kjt.lms.repository.OtpRepository;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final MessageProvider messageProvider;
    private final EmailService emailService;

    @Value("${otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.rate-limit-requests-per-hour:5}")
    private int rateLimitRequestsPerHour;

    private static final String OTP_PATTERN = "[0-9]{6}";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSaveOtp(String email, String purpose) {
        // Check rate limiting
        checkRateLimit(email, purpose);

        // Invalidate existing OTP
        otpRepository.findByEmailAndPurposeAndIsUsedFalse(email, purpose)
                .ifPresent(otp -> {
                    otp.setIsUsed(true);
                    otpRepository.save(otp);
                });

        // Generate secure random OTP (6 digits)
        String otpCode = generateSecureOtp();

        // Create and save OTP entity
        OtpEntity otp = OtpEntity.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiredAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .failedAttempts(0)
                .maxAttempts(maxAttempts)
                .isUsed(false)
                .build();

        otpRepository.save(otp);
        log.info("OTP generated for email: {} with purpose: {}", email, purpose);

        // Send OTP via email asynchronously
        emailService.sendOtpEmail(email, otpCode, purpose);

    }

    @Override
    @Transactional
    public void validateOtp(String email, String purpose, String otpCode) {
        // Validate OTP format
        if (!otpCode.matches(OTP_PATTERN)) {
            throw new BusinessException(messageProvider.getMessage("auth.verifyOtp.invalid"));
        }

        // Find OTP record
        OtpEntity otp = otpRepository.findByEmailAndPurposeAndIsUsedFalse(email, purpose)
                .orElseThrow(() -> new BusinessException(messageProvider.getMessage("auth.verifyOtp.invalid")));

        // Check if expired
        if (otp.isExpired()) {
            otp.setIsUsed(true);
            otpRepository.save(otp);
            throw new BusinessException(messageProvider.getMessage("otp.expired"));
        }

        // Check max attempts exceeded
        if (otp.isMaxAttemptsExceeded()) {
            otp.setIsUsed(true);
            otpRepository.save(otp);
            throw new BusinessException(messageProvider.getMessage("otp.max_attempts_exceeded"));
        }

        // Verify OTP code
        if (!otp.getOtpCode().equals(otpCode)) {
            otp.incrementFailedAttempts();
            otpRepository.save(otp);

            int remainingAttempts = otp.getMaxAttempts() - otp.getFailedAttempts();
            if (remainingAttempts <= 0) {
                throw new BusinessException(messageProvider.getMessage("otp.max_attempts_exceeded"));
            }

            throw new BusinessException(messageProvider.getMessage("otp.invalid", remainingAttempts));
        }

        // Mark OTP as used
        otp.markAsUsed();
        otpRepository.save(otp);
        log.info("OTP verified for email: {} with purpose: {}", email, purpose);
    }

    @Override
    @Transactional
    public void deleteExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
        log.info("Expired OTPs deleted");
    }

    private String generateSecureOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private void checkRateLimit(String email, String purpose) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long requestsInLastHour = otpRepository.countByEmailAndPurposeAndCreatedAtAfter(email, purpose, oneHourAgo);

        if (requestsInLastHour >= rateLimitRequestsPerHour) {
            throw new BusinessException(messageProvider.getMessage("otp.rate_limit_exceeded"));
        }
    }
}

