package com.stayease.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest
{
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}