package com.stayease.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest
{
    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Guests must be at least 1")
    private int totalGuests = 1;

    @Min(value = 1, message = "Rooms must be at least 1")
    private int totalRooms = 1;
}