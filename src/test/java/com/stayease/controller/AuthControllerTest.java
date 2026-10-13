package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.dto.RegisterRequest;
import com.stayease.dto.ResetPasswordRequest;
import com.stayease.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AuthControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void showRegisterPageReturnsRegisterView() throws Exception
    {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    void registerSuccessfullyRedirectsToLogin() throws Exception
    {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(authService).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsMissingFields() throws Exception
    {
        mockMvc.perform(post("/register")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasErrors("registerRequest"));

        verify(authService, never()).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception
    {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someonegmail")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "userEmail"));

        verify(authService, never()).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsNameShorterThanThreeCharacters() throws Exception
    {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Te")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "userName"));

        verify(authService, never()).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsPasswordShorterThanEightCharacters() throws Exception
    {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "1234")
                        .param("userConfirmPassword", "1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "userPassword"));

        verify(authService, never()).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerDisplaysDuplicateEmailError() throws Exception
    {
        doThrow(new RuntimeException("Email already registered!")).when(authService).userRegister(any(RegisterRequest.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "Email already registered!"));

        verify(authService).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerDisplaysPasswordMismatchError() throws Exception
    {
        doThrow(new RuntimeException("Passwords do not match!")).when(authService).userRegister(any(RegisterRequest.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "Passwords do not match!"));

        verify(authService).userRegister(any(RegisterRequest.class));
    }

    @Test
    void registerDisplaysUnexpectedServiceError() throws Exception
    {
        doThrow(new RuntimeException("Registration failed!")).when(authService).userRegister(any(RegisterRequest.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("userName", "Test")
                        .param("userEmail", "someone@gmail.com")
                        .param("userPassword", "Password123")
                        .param("userConfirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "Registration failed!"));
    }

    @Test
    void showLoginPageReturnsLoginView() throws Exception
    {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void showLoginPageDisplaysLoginError() throws Exception
    {
        mockMvc.perform(get("/login")
                        .param("error", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid email, password, or your account is disabled!"));
    }

    @Test
    void showLoginPageDisplaysLogoutMessage() throws Exception
    {
        mockMvc.perform(get("/login")
                        .param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "You have been logged out successfully."));
    }

    @Test
    void showLoginPageDisplaysRegistrationMessage() throws Exception
    {
        mockMvc.perform(get("/login")
                        .param("registered", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "Registration successful. Please login."));
    }

    @Test
    void showLoginPageDisplaysPasswordResetMessage() throws Exception
    {
        mockMvc.perform(get("/login")
                        .param("reset", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "Password reset successfully. Please login."));
    }

    @Test
    void forgotPasswordPageReturnsCorrectView() throws Exception
    {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    @Test
    void resetPasswordPageRejectsInvalidToken() throws Exception
    {
        when(authService.isValidToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/reset-password")
                        .with(csrf())
                        .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("resetPasswordRequest"));

        verify(authService).isValidToken("invalid-token");
    }

    @Test
    void resetPasswordPageAcceptsValidToken() throws Exception
    {
        when(authService.isValidToken("valid-token")).thenReturn(true);

        mockMvc.perform(get("/reset-password")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "valid-token"))
                .andExpect(model().attributeExists("resetPasswordRequest"));
    }

    @Test
    void sendResetLinkCallsServiceWithTrimmedEmail() throws Exception
    {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", "  customer@example.com  "))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("message"));

        verify(authService).createResetToken("customer@example.com");
    }

    @Test
    void resetPasswordRejectsInvalidToken() throws Exception
    {
        when(authService.isValidToken("expired-token")).thenReturn(false);

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "expired-token")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("error"));

        verify(authService, never()).resetPassword(anyString(), any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordRejectsPasswordValidationErrors() throws Exception
    {
        when(authService.isValidToken("valid-token")).thenReturn(true);

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "valid-token")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "valid-token"));

        verify(authService, never()).resetPassword(anyString(), any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordRedirectsAfterSuccessfulReset() throws Exception
    {
        when(authService.isValidToken("valid-token")).thenReturn(true);

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "valid-token")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset"));

        verify(authService).resetPassword(eq("valid-token"), any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordDisplaysServiceError() throws Exception
    {
        when(authService.isValidToken("valid-token")).thenReturn(true);

        doThrow(new RuntimeException("New password must be different from your current password")).when(authService).resetPassword(eq("valid-token"), any(ResetPasswordRequest.class));

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "valid-token")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("error", "New password must be different from your current password"))
                .andExpect(model().attribute("token", "valid-token"));
    }
}