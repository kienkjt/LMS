package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.auth.ForgotPasswordRequestDto;
import com.kjt.lms.model.request.auth.LoginRequestDto;
import com.kjt.lms.model.request.auth.RefreshTokenRequestDto;
import com.kjt.lms.model.request.auth.RegistrationRequestDto;
import com.kjt.lms.model.request.auth.ResetPasswordRequestDto;
import com.kjt.lms.model.request.auth.VerifyOtpRequestDto;
import com.kjt.lms.model.response.auth.LoginResponseDto;
import com.kjt.lms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MessageProvider messageProvider;

    @PostMapping("/register")
    public ResponseEntity<APIResponse<Void>> register(@Valid @RequestBody RegistrationRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(null, messageProvider.getMessage("auth.register.success")));
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(APIResponse.success(response, response.getMessage()));
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("auth.logout.success")));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<APIResponse<LoginResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        LoginResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(APIResponse.success(response, response.getMessage()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<APIResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("auth.verifyOtp.success")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("auth.forgotPassword.success")));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<APIResponse<Void>> verifyResetOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
        authService.verifyResetOtp(request);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("auth.verifyResetOtp.success")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("auth.resetPassword.success")));
    }
}
