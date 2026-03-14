package com.kjt.lms.service;

public interface OtpService {
    void generateAndSaveOtp(String email, String purpose);
    void validateOtp(String email, String purpose, String otpCode);
    void deleteExpiredOtps();
}

