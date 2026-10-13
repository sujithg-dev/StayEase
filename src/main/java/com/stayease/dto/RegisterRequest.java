package com.stayease.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest
{
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 30, message = "Name must be between 3 and 30 characters")
    private String userName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String userEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 to 50 characters")
    private String userPassword;

    @NotBlank(message = "Please confirm your password")
    private String userConfirmPassword;
}