package com.kjt.lms.service;

public interface EmailService {
    void sendOtpEmail(String email, String otpCode, String purpose);
    void sendWelcomeEmail(String email, String fullName);
    void sendPasswordResetEmail(String email, String fullName);
}

