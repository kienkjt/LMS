package com.kjt.lms.service.impl;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.JwtUtil;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.DuplicateResourceException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.RoleEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.auth.ForgotPasswordRequestDto;
import com.kjt.lms.model.request.auth.LoginRequestDto;
import com.kjt.lms.model.request.auth.RefreshTokenRequestDto;
import com.kjt.lms.model.request.auth.RegistrationRequestDto;
import com.kjt.lms.model.request.auth.ResetPasswordRequestDto;
import com.kjt.lms.model.request.auth.VerifyOtpRequestDto;
import com.kjt.lms.model.response.LoginResponseDto;
import com.kjt.lms.repository.RoleRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.AuthService;
import com.kjt.lms.service.OtpService;
import com.kjt.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final MessageProvider messageProvider;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public void register(RegistrationRequestDto request) {
        UserEntity existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            if(Boolean.TRUE.equals(existingUser.getIsVerified())){
                throw new DuplicateResourceException(messageProvider.getMessage("auth.register.emailExists"));
            }
            otpService.generateAndSaveOtp(request.getEmail(),"REGISTRATION");

            log.info("Resend OTP for unverified user: {}", request.getEmail());
            return;
        }

        // Get role by code (STUDENT or INSTRUCTOR)
        RoleEntity role = roleRepository.findByCode(request.getRole().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.register.roleNotFound")));

        // Create user
        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roleId(role.getId())
                .isVerified(false)
                .active(CommonStatusEnum.ACTIVE)
                .build();

        userRepository.save(user);

        // Generate and send OTP
        otpService.generateAndSaveOtp(request.getEmail(), "REGISTRATION");

        log.info("User registered: {} with role: {}", user.getEmail(), request.getRole());
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserEntity user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.login.emailNotFound")));

            if (!user.getIsVerified()) {
                throw new BusinessException(messageProvider.getMessage("auth.login.notVerified"));
            }

            if (user.getActive() != CommonStatusEnum.ACTIVE) {
                throw new BusinessException(messageProvider.getMessage("auth.login.inactive"));
            }

            String accessToken = jwtUtil.generateToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
            String role = jwtUtil.getRoleFromToken(accessToken);

            return LoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs)
                    .role(role)
                    .message(messageProvider.getMessage("auth.login.success"))
                    .build();

        } catch (BadCredentialsException e) {
            throw new BusinessException(messageProvider.getMessage("auth.login.invalidCredentials"));
        }
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Override
    public LoginResponseDto refreshToken(RefreshTokenRequestDto request) {
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new BusinessException(messageProvider.getMessage("auth.refresh.invalid"));
        }

        String email = jwtUtil.getEmailFromToken(request.getRefreshToken());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.login.emailNotFound")));

        if (!user.getIsVerified() || user.getActive() != CommonStatusEnum.ACTIVE) {
            throw new BusinessException(messageProvider.getMessage("auth.refresh.accountInactive"));
        }

        String role = roleRepository.findById(user.getRoleId())
                .map(r -> r.getCode().toUpperCase())
                .orElse("STUDENT");

        String newAccessToken = jwtUtil.generateTokenFromEmail(email, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .role(role)
                .message(messageProvider.getMessage("auth.refresh.success"))
                .build();
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequestDto request) {
        String email = request.getEmail();

        // Validate OTP
        otpService.validateOtp(email, "REGISTRATION", request.getOtp());

        // Mark user as verified
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.login.emailNotFound")));

        user.setIsVerified(true);
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        log.info("User verified: {}", email);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDto request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.forgotPassword.userNotFound")));

        // Generate and send password reset OTP
        otpService.generateAndSaveOtp(user.getEmail(), "PASSWORD_RESET");

        log.info("Forgot password OTP sent to: {}", user.getEmail());
    }

    @Override
    public void verifyResetOtp(VerifyOtpRequestDto request) {
        String email = request.getEmail();
        otpService.validateOtp(email, "PASSWORD_RESET", request.getOtp());
        otpService.markPasswordResetVerified(email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        otpService.requirePasswordResetVerified(request.getEmail());

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("auth.login.emailNotFound")));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpService.clearPasswordResetVerified(request.getEmail());

        // Send password reset confirmation email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName());

        log.info("Password reset for user: {}", request.getEmail());
    }
}
