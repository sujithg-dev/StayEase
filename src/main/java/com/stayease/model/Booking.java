package com.stayease.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking
{
    @Id
    private String bookingId;

    private String bookingNumber;

    private String customerId;

    private String customerName;

    private String customerEmail;

    private String hotelId;

    private String hotelName;

    private String roomId;

    private String roomType;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private int totalGuests;

    private int totalRooms;

    private BigDecimal roomPrice;

    private BigDecimal totalAmount;

    private String paymentId;

    private String paymentMode;

    private BookingStatus bookingStatus = BookingStatus.PENDING_PAYMENT;

    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime createdAt;

    public enum BookingStatus
    {
        PENDING_PAYMENT,
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }

    public enum PaymentStatus
    {
        PENDING,
        PAID,
        FAILED,
        REFUNDED
    }
}