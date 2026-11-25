package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.DuplicateResourceException;
import com.kjt.lms.exception.GlobalExceptionHandler;
import com.kjt.lms.model.response.auth.LoginResponseDto;
import com.kjt.lms.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MessageProvider messageProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        messageProvider = mock(MessageProvider.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, messageProvider))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturnsCreatedAndCallsService() throws Exception {
        when(messageProvider.getMessage("auth.register.success")).thenReturn("Registered");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@example.com","password":"Password1!",
                                 "fullName":"Student Name","role":"STUDENT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registered"));

        verify(authService).register(any());
    }

    @Test
    void registerRejectsInvalidInputBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid","password":"weak","fullName":"A","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.role").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void registerMapsDuplicateEmailToConflict() throws Exception {
        doThrow(new DuplicateResourceException("Email exists")).when(authService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@example.com","password":"Password1!",
                                 "fullName":"Student Name","role":"STUDENT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email exists"));
    }

    @Test
    void loginReturnsTokensFromService() throws Exception {
        when(authService.login(any())).thenReturn(LoginResponseDto.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .role("ROLE_STUDENT")
                .message("Logged in")
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@example.com","password":"Password1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"));
    }

    @Test
    void loginRejectsMalformedEmailBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"Password1!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void verifyOtpRejectsNonNumericCodeBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@example.com","otp":"abcdef"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.otp").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void refreshTokenReturnsRotatedCredentials() throws Exception {
        when(authService.refreshToken(any())).thenReturn(LoginResponseDto.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .message("Refreshed")
                .build());

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0In0.signature"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }
}
