package com.stayease.service;

import com.stayease.dto.RegisterRequest;
import com.stayease.dto.ResetPasswordRequest;
import com.stayease.model.Token;
import com.stayease.model.User;
import com.stayease.repository.TokenRepository;
import com.stayease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public void userRegister(RegisterRequest request)
    {
        if (userRepository.existsByUserEmail(request.getUserEmail()))
        {
            throw new RuntimeException("Email already registered!");
        }

        if (!request.getUserPassword().equals(request.getUserConfirmPassword()))
        {
            throw new RuntimeException("Passwords do not match!");
        }

        User user = new User();

        user.setUserName(request.getUserName());
        user.setUserEmail(request.getUserEmail());
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserRole(User.UserRole.CUSTOMER);
        user.setUserStatus(User.UserStatus.ACTIVE);

        userRepository.save(user);
    }

    public void createResetToken(String email)
    {
        User user = userRepository.findByUserEmail(email).orElse(null);

        if (user == null)
        {
            return;
        }

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        tokenRepository
                .findAll()
                .stream().
                filter(token ->
                        token.getUserId()
                                .equals(user.getUserId()) && !token.isUsed())
                .forEach(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });


        Token resetToken = new Token();

        resetToken.setUserId(user.getUserId());
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, rawToken);
    }

    public boolean isValidToken(String token)
    {
        if (token == null || token.isBlank())
        {
            return false;
        }

        String tokenHash = hashToken(token);
        Token resetToken = tokenRepository.findByTokenHash(tokenHash).orElse(null);

        if (resetToken == null)
        {
            return false;
        }

        if (resetToken.isUsed())
        {
            return false;
        }

        return LocalDateTime.now().isBefore(resetToken.getExpiryTime());
    }

    public void resetPassword(String token, ResetPasswordRequest request)
    {
        if (!isValidToken(token))
        {
            throw new RuntimeException("This password reset link is invalid or has expired!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
        {
            throw new RuntimeException("New password and confirm password do not match!");
        }

        String tokenHash = hashToken(token);

        Token resetToken = tokenRepository.findByTokenHash(tokenHash).orElseThrow(() -> new RuntimeException("Invalid password reset token!"));

        User user = userRepository.findById(resetToken.getUserId()).orElseThrow(() -> new RuntimeException("User not found!"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getUserPassword()))
        {
            throw new RuntimeException("New password must be different from your current password!");
        }

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        resetToken.setUsed(true);

        tokenRepository.save(resetToken);
    }

    private String hashToken(String token)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : hash)
            {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new RuntimeException("Unable to generate token hash!", exception);
        }
    }
}