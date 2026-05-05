package com.kjt.lms.service.impl;

import com.kjt.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from:noreply@lms.com}")
    private String mailFrom;

    @Value("${app.name:LMS System}")
    private String appName;

    @Override
    @Async
    public void sendOtpEmail(String email, String otpCode, String purpose) {
        try {
            String subject;
            String templateName;

            if ("REGISTRATION".equalsIgnoreCase(purpose)) {
                subject = "Xác thực tài khoản - LMS";
                templateName = "emails/verify-otp";
            } else if ("PASSWORD_RESET".equalsIgnoreCase(purpose)) {
                subject = "Đặt lại mật khẩu - LMS";
                templateName = "emails/reset-otp";
            } else {
                subject = "Xác thực email - LMS";
                templateName = "emails/verify-otp";
            }

            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            context.setVariable("expirationMinutes", 5);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process(templateName, context);

            sendHtmlEmail(email, subject, htmlContent);

            log.info("Đã gửi email OTP tới: {} cho mục đích: {}", email, purpose);

        } catch (Exception e) {
            log.error("Không thể gửi email OTP tới: {} cho mục đích: {}", email, purpose, e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String email, String fullName) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("emails/welcome", context);

            sendHtmlEmail(email, "Chào mừng bạn đến với " + appName, htmlContent);

            log.info("Đã gửi email chào mừng tới: {}", email);

        } catch (Exception e) {
            log.error("Không thể gửi email chào mừng tới: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String fullName) {
        // ...existing code...
    }

    @Override
    @Async
    public void sendAccountLockedEmail(String email, String fullName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("reason", reason != null ? reason : "Không có lý do được cung cấp");
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("emails/account-locked", context);

            sendHtmlEmail(email, "Tài khoản của bạn đã bị khóa - " + appName, htmlContent);

            log.info("Đã gửi email thông báo khóa tài khoản tới: {}", email);

        } catch (Exception e) {
            log.error("Không thể gửi email thông báo khóa tài khoản tới: {}", email, e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}