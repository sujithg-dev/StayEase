package com.stayease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileRequest
{
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 30, message = "Name must be between 3 and 30 characters")
    private String userName;
}