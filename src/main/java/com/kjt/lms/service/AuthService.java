package com.kjt.lms.service;

import com.kjt.lms.model.request.auth.ForgotPasswordRequestDto;
import com.kjt.lms.model.request.auth.LoginRequestDto;
import com.kjt.lms.model.request.auth.RefreshTokenRequestDto;
import com.kjt.lms.model.request.auth.RegistrationRequestDto;
import com.kjt.lms.model.request.auth.ResetPasswordRequestDto;
import com.kjt.lms.model.request.auth.VerifyOtpRequestDto;
import com.kjt.lms.model.response.LoginResponseDto;

public interface AuthService {
    void register(RegistrationRequestDto request);
    LoginResponseDto login(LoginRequestDto request);
    void logout();
    LoginResponseDto refreshToken(RefreshTokenRequestDto request);
    void verifyOtp(VerifyOtpRequestDto request);
    void forgotPassword(ForgotPasswordRequestDto request);
    void verifyResetOtp(VerifyOtpRequestDto request);
    void resetPassword(ResetPasswordRequestDto request);
}
