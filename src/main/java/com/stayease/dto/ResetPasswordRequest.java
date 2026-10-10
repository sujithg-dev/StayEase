package com.stayease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest
{
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 to 50 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}