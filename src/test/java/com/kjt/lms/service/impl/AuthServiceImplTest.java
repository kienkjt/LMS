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
import com.kjt.lms.model.response.auth.LoginResponseDto;
import com.kjt.lms.repository.RoleRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "student@example.com";
    private static final String PASSWORD = "Password1!";
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private MessageProvider messageProvider;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;
    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3_600_000L);
    }

    @Test
    void registerCreatesUnverifiedUserWithEncodedPasswordAndSendsOtp() {
        RegistrationRequestDto request = registration();
        RoleEntity role = RoleEntity.builder().code("STUDENT").build();
        role.setId(ROLE_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(saved.capture());
        assertAll(
                () -> assertEquals(EMAIL, saved.getValue().getEmail()),
                () -> assertEquals("encoded-password", saved.getValue().getPassword()),
                () -> assertEquals("Student Name", saved.getValue().getFullName()),
                () -> assertEquals(ROLE_ID, saved.getValue().getRoleId()),
                () -> assertFalse(saved.getValue().getIsVerified()),
                () -> assertEquals(CommonStatusEnum.ACTIVE, saved.getValue().getActive())
        );
        verify(otpService).generateAndSaveOtp(EMAIL, "REGISTRATION");
    }

    @Test
    void registerRejectsVerifiedEmailWithoutSendingOtp() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(true)));

        assertThrows(DuplicateResourceException.class, () -> authService.register(registration()));

        verifyNoInteractions(roleRepository, passwordEncoder, otpService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerResendsOtpForUnverifiedEmailWithoutCreatingAnotherUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(false)));

        authService.register(registration());

        verify(otpService).generateAndSaveOtp(EMAIL, "REGISTRATION");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void registerRejectsUnknownRoleBeforeSavingUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(registration()));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, otpService);
    }

    @Test
    void verifyOtpActivatesAccountAndSendsWelcomeEmail() {
        UserEntity user = user(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.verifyOtp(verification());

        verify(otpService).validateOtp(EMAIL, "REGISTRATION", "123456");
        assertTrue(user.getIsVerified());
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail(EMAIL, "Student Name");
    }

    @Test
    void verifyOtpFailureDoesNotActivateAccount() {
        doThrow(new BusinessException("invalid otp"))
                .when(otpService).validateOtp(EMAIL, "REGISTRATION", "123456");

        assertThrows(BusinessException.class, () -> authService.verifyOtp(verification()));

        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    void loginReturnsBothTokensForVerifiedActiveUser() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(true)));
        when(jwtUtil.generateToken(authentication)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn("refresh-token");
        when(jwtUtil.getRoleFromToken("access-token")).thenReturn("ROLE_STUDENT");

        LoginResponseDto response = authService.login(login());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> credentials =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(credentials.capture());
        assertEquals(EMAIL, credentials.getValue().getPrincipal());
        assertEquals(PASSWORD, credentials.getValue().getCredentials());
        assertAll(
                () -> assertEquals("access-token", response.getAccessToken()),
                () -> assertEquals("refresh-token", response.getRefreshToken()),
                () -> assertEquals("Bearer", response.getTokenType()),
                () -> assertEquals(3_600_000L, response.getExpiresIn()),
                () -> assertEquals("ROLE_STUDENT", response.getRole())
        );
    }

    @Test
    void loginRejectsWrongPasswordWithoutIssuingTokens() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BusinessException.class, () -> authService.login(login()));

        verifyNoInteractions(userRepository, jwtUtil);
    }

    @Test
    void loginRejectsAccountMissingAfterAuthentication() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(login()));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginRejectsUnverifiedAccountWithoutIssuingTokens() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(false)));

        assertThrows(BusinessException.class, () -> authService.login(login()));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginRejectsLockedAccountWithoutIssuingTokens() {
        UserEntity user = user(true);
        user.setIsLocked(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.login(login()));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginRejectsInactiveAccountWithoutIssuingTokens() {
        UserEntity user = user(true);
        user.setActive(CommonStatusEnum.INACTIVE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.login(login()));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void refreshRejectsInvalidTokenWithoutLookingUpUser() {
        when(jwtUtil.validateToken("invalid")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.refreshToken(refresh("invalid")));

        verifyNoInteractions(userRepository, roleRepository);
    }

    @Test
    void refreshRotatesTokensForActiveAccount() {
        UserEntity user = user(true);
        RoleEntity role = RoleEntity.builder().code("STUDENT").build();
        when(jwtUtil.validateToken("old-refresh")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("old-refresh")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        when(jwtUtil.generateTokenFromEmail(EMAIL, "STUDENT")).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn("new-refresh");

        LoginResponseDto response = authService.refreshToken(refresh("old-refresh"));

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("STUDENT", response.getRole());
        assertEquals(3_600_000L, response.getExpiresIn());
    }

    @Test
    void refreshRejectsLockedAccountWithoutIssuingTokens() {
        UserEntity user = user(true);
        user.setIsLocked(true);
        when(jwtUtil.validateToken("old-refresh")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("old-refresh")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.refreshToken(refresh("old-refresh")));

        verify(jwtUtil, never()).generateTokenFromEmail(anyString(), anyString());
        verify(jwtUtil, never()).generateRefreshToken(anyString());
    }

    @Test
    void refreshRejectsUnverifiedAccountWithoutIssuingTokens() {
        when(jwtUtil.validateToken("old-refresh")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("old-refresh")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(false)));

        assertThrows(BusinessException.class, () -> authService.refreshToken(refresh("old-refresh")));

        verify(jwtUtil, never()).generateTokenFromEmail(anyString(), anyString());
    }

    @Test
    void refreshRejectsInactiveAccountWithoutIssuingTokens() {
        UserEntity user = user(true);
        user.setActive(CommonStatusEnum.INACTIVE);
        when(jwtUtil.validateToken("old-refresh")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("old-refresh")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.refreshToken(refresh("old-refresh")));

        verify(jwtUtil, never()).generateTokenFromEmail(anyString(), anyString());
    }

    @Test
    void forgotPasswordSendsResetOtpOnlyForExistingAccount() {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(true)));

        authService.forgotPassword(request);

        verify(otpService).generateAndSaveOtp(EMAIL, "PASSWORD_RESET");
    }

    @Test
    void verifyResetOtpCreatesTemporaryResetPermission() {
        authService.verifyResetOtp(verification());

        var order = inOrder(otpService);
        order.verify(otpService).validateOtp(EMAIL, "PASSWORD_RESET", "123456");
        order.verify(otpService).markPasswordResetVerified(EMAIL);
    }

    @Test
    void verifyResetOtpFailureDoesNotGrantResetPermission() {
        doThrow(new BusinessException("invalid otp"))
                .when(otpService).validateOtp(EMAIL, "PASSWORD_RESET", "123456");

        assertThrows(BusinessException.class, () -> authService.verifyResetOtp(verification()));

        verify(otpService, never()).markPasswordResetVerified(anyString());
    }

    @Test
    void resetPasswordRequiresVerifiedOtpBeforeChangingPassword() {
        doThrow(new BusinessException("otp required"))
                .when(otpService).requirePasswordResetVerified(EMAIL);

        assertThrows(BusinessException.class, () -> authService.resetPassword(reset()));

        verifyNoInteractions(userRepository, passwordEncoder, emailService);
    }

    @Test
    void resetPasswordEncodesPasswordAndClearsResetPermission() {
        UserEntity user = user(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-hash");

        authService.resetPassword(reset());

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
        verify(otpService).clearPasswordResetVerified(EMAIL);
        verify(emailService).sendPasswordResetEmail(EMAIL, "Student Name");
    }

    @Test
    void logoutClearsSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static RegistrationRequestDto registration() {
        RegistrationRequestDto request = new RegistrationRequestDto();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFullName("Student Name");
        request.setRole("STUDENT");
        return request;
    }

    private static LoginRequestDto login() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private static VerifyOtpRequestDto verification() {
        VerifyOtpRequestDto request = new VerifyOtpRequestDto();
        request.setEmail(EMAIL);
        request.setOtp("123456");
        return request;
    }

    private static RefreshTokenRequestDto refresh(String token) {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(token);
        return request;
    }

    private static ResetPasswordRequestDto reset() {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail(EMAIL);
        request.setNewPassword("NewPassword1!");
        return request;
    }

    private static UserEntity user(boolean verified) {
        return UserEntity.builder()
                .email(EMAIL)
                .password("old-hash")
                .fullName("Student Name")
                .roleId(ROLE_ID)
                .isVerified(verified)
                .isLocked(false)
                .active(CommonStatusEnum.ACTIVE)
                .build();
    }
}
