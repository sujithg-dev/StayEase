package com.stayease.service;

import com.stayease.dto.ChangePasswordRequest;
import com.stayease.dto.ProfileRequest;
import com.stayease.model.User;
import com.stayease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserByUserEmail(String userEmail)
    {
        return userRepository.findByUserEmail(userEmail).orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public Optional<User> getUserByUserId(String userId)
    {
        return userRepository.findById(userId);
    }

    public void updateProfile(String userEmail, ProfileRequest request)
    {
        User user = getUserByUserEmail(userEmail);

        user.setUserName(request.getUserName().trim());

        userRepository.save(user);
    }

    public void changePassword(String email, ChangePasswordRequest request)
    {
        User user = getUserByUserEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getUserPassword()))
        {
            throw new RuntimeException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getUserPassword()))
        {
            throw new RuntimeException("New password must be different from your current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
        {
            throw new RuntimeException("New password and confirm password do not match");
        }

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }
}