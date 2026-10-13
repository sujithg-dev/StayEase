package com.stayease.service;

import com.stayease.dto.RegisterRequest;
import com.stayease.dto.ResetPasswordRequest;
import com.stayease.model.Token;
import com.stayease.model.User;
import com.stayease.repository.TokenRepository;
import com.stayease.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp()
    {
        user = new User();
        user.setUserId("user123");
        user.setUserName("Test User");
        user.setUserEmail("test@example.com");
        user.setUserPassword("encodedOldPassword");
    }

    private RegisterRequest registerRequest()
    {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("Test User");
        request.setUserEmail("test@example.com");
        request.setUserPassword("Password@123");
        request.setUserConfirmPassword("Password@123");
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest(String confirmPassword)
    {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    private String hashToken(String token) throws Exception
    {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();

        for (byte b : hash)
        {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    private Token validToken(String rawToken) throws Exception
    {
        Token token = new Token();
        token.setUserId(user.getUserId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiryTime(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);
        return token;
    }

    @Test
    void userRegister_whenValidRequest_savesCustomer()
    {
        RegisterRequest request = registerRequest();

        when(userRepository.existsByUserEmail(request.getUserEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getUserPassword())).thenReturn("encodedPassword");

        authService.userRegister(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals("Test User", savedUser.getUserName());
        assertEquals("test@example.com", savedUser.getUserEmail());
        assertEquals("encodedPassword", savedUser.getUserPassword());
        assertEquals(User.UserRole.CUSTOMER, savedUser.getUserRole());
        assertEquals(User.UserStatus.ACTIVE, savedUser.getUserStatus());
    }

    @Test
    void userRegister_whenEmailAlreadyExists_throwsException()
    {
        RegisterRequest request = registerRequest();

        when(userRepository.existsByUserEmail(request.getUserEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.userRegister(request));

        assertEquals("Email already registered!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void userRegister_whenPasswordsDoNotMatch_throwsException()
    {
        RegisterRequest request = registerRequest();
        request.setUserConfirmPassword("DifferentPassword@123");

        when(userRepository.existsByUserEmail(request.getUserEmail())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.userRegister(request));

        assertEquals("Passwords do not match!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createResetToken_whenUserDoesNotExist_doesNothing()
    {
        when(userRepository.findByUserEmail("missing@example.com")).thenReturn(Optional.empty());

        authService.createResetToken("missing@example.com");

        verify(tokenRepository, never()).save(any(Token.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void createResetToken_whenUserExists_savesTokenAndSendsEmail() throws Exception
    {
        when(userRepository.findByUserEmail(user.getUserEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findAll()).thenReturn(List.of());

        authService.createResetToken(user.getUserEmail());

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        Token savedToken = tokenCaptor.getValue();

        assertEquals(user.getUserId(), savedToken.getUserId());
        assertNotNull(savedToken.getTokenHash());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiryTime().isAfter(LocalDateTime.now()));
        assertTrue(savedToken.getExpiryTime().isBefore(LocalDateTime.now().plusMinutes(31)));

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(user), rawTokenCaptor.capture());

        String rawToken = rawTokenCaptor.getValue();

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertEquals(savedToken.getTokenHash(), hashToken(rawToken));
    }

    @Test
    void createResetToken_whenUnusedTokensExist_invalidatesPreviousTokens()
    {
        Token oldToken = new Token();
        oldToken.setUserId(user.getUserId());
        oldToken.setUsed(false);

        Token usedToken = new Token();
        usedToken.setUserId(user.getUserId());
        usedToken.setUsed(true);

        when(userRepository.findByUserEmail(user.getUserEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findAll()).thenReturn(List.of(oldToken, usedToken));

        authService.createResetToken(user.getUserEmail());

        assertTrue(oldToken.isUsed());

        verify(tokenRepository, times(2)).save(any(Token.class));
        verify(tokenRepository).save(same(oldToken));
        verify(tokenRepository, never()).save(same(usedToken));
        verify(emailService).sendPasswordResetEmail(eq(user), anyString());
    }

    @Test
    void isValidToken_whenTokenIsNull_returnsFalse()
    {
        assertFalse(authService.isValidToken(null));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void isValidToken_whenTokenIsBlank_returnsFalse()
    {
        assertFalse(authService.isValidToken("  "));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void isValidToken_whenTokenDoesNotExist_returnsFalse() throws Exception
    {
        String rawToken = "invalid-token";

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.empty());

        assertFalse(authService.isValidToken(rawToken));
    }

    @Test
    void isValidToken_whenTokenIsUsed_returnsFalse() throws Exception
    {
        String rawToken = "used-token";
        Token token = validToken(rawToken);
        token.setUsed(true);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));

        assertFalse(authService.isValidToken(rawToken));
    }

    @Test
    void isValidToken_whenTokenIsExpired_returnsFalse() throws Exception
    {
        String rawToken = "expired-token";
        Token token = validToken(rawToken);
        token.setExpiryTime(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));

        assertFalse(authService.isValidToken(rawToken));
    }

    @Test
    void isValidToken_whenTokenIsValid_returnsTrue() throws Exception
    {
        String rawToken = "valid-token";
        Token token = validToken(rawToken);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));

        assertTrue(authService.isValidToken(rawToken));
    }

    @Test
    void resetPassword_whenTokenIsInvalid_throwsException()
    {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService
                .resetPassword("invalid-token", resetPasswordRequest("NewPassword@123")));

        assertEquals("This password reset link is invalid or has expired!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenPasswordsDoNotMatch_throwsException() throws Exception
    {
        String rawToken = "valid-token";
        Token token = validToken(rawToken);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService
                .resetPassword(rawToken, resetPasswordRequest("DifferentPassword@123")));

        assertEquals("New password and confirm password do not match!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenUserDoesNotExist_throwsException() throws Exception
    {
        String rawToken = "valid-token";
        Token token = validToken(rawToken);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService
                .resetPassword(rawToken, resetPasswordRequest("NewPassword@123")));

        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void resetPassword_whenNewPasswordMatchesCurrentPassword_throwsException() throws Exception
    {
        String rawToken = "valid-token";
        Token token = validToken(rawToken);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPassword@123", user.getUserPassword())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService
                .resetPassword(rawToken, resetPasswordRequest("NewPassword@123")));

        assertEquals("New password must be different from your current password!", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void resetPassword_whenValidRequest_updatesPasswordAndMarksTokenUsed() throws Exception
    {
        String rawToken = "valid-token";
        Token token = validToken(rawToken);

        when(tokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPassword@123", user.getUserPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("encodedNewPassword");

        authService.resetPassword(rawToken, resetPasswordRequest("NewPassword@123"));

        assertEquals("encodedNewPassword", user.getUserPassword());
        assertTrue(token.isUsed());

        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }
}