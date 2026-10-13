
package com.stayease.service;

import com.stayease.dto.ChangePasswordRequest;
import com.stayease.dto.ProfileRequest;
import com.stayease.model.User;
import com.stayease.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

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

    @Test
    void getUserByUserEmail_whenUserExists_returnsUser()
    {
        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByUserEmail("test@example.com");

        assertSame(user, result);
        verify(userRepository).findByUserEmail("test@example.com");
    }

    @Test
    void getUserByUserEmail_whenUserDoesNotExist_throwsException()
    {
        when(userRepository.findByUserEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserByUserEmail("missing@example.com"));

        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void getUserByUserId_whenUserExists_returnsOptionalUser()
    {
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByUserId("user123");

        assertTrue(result.isPresent());
        assertSame(user, result.get());
    }

    @Test
    void getUserByUserId_whenUserDoesNotExist_returnsEmptyOptional()
    {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserByUserId("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateProfile_whenValidRequest_updatesTrimmedUserName()
    {
        ProfileRequest request = new ProfileRequest();
        request.setUserName("  Updated Name  ");

        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.updateProfile("test@example.com", request);

        assertEquals("Updated Name", user.getUserName());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_whenCurrentPasswordIsIncorrect_throwsException()
    {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword("test@example.com", request));

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_whenNewPasswordMatchesCurrentPassword_throwsException()
    {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword@123");
        request.setNewPassword("OldPassword@123");
        request.setConfirmPassword("OldPassword@123");

        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "encodedOldPassword")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword("test@example.com", request));

        assertEquals("New password must be different from your current password", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_whenNewPasswordsDoNotMatch_throwsException()
    {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("DifferentPassword@123");

        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@123", "encodedOldPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword("test@example.com", request));

        assertEquals("New password and confirm password do not match", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_whenRequestIsValid_updatesEncodedPassword()
    {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@123", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("encodedNewPassword");

        userService.changePassword("test@example.com", request);

        assertEquals("encodedNewPassword", user.getUserPassword());
        verify(userRepository).save(user);
        verify(passwordEncoder).encode("NewPassword@123");
    }
}
