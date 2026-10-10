package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService
{
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    public Booking processPayment(String bookingId, String customerId, String paymentMethod)
    {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId))
        {
            throw new RuntimeException("You are not allowed to pay for this booking");
        }

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED)
        {
            throw new RuntimeException("Cancelled booking cannot be paid");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.PENDING_PAYMENT)
        {
            throw new RuntimeException("This booking is not awaiting payment");
        }

        if (booking.getPaymentStatus() == Booking.PaymentStatus.PAID)
        {
            throw new RuntimeException("This booking has already been paid");
        }

        if (!paymentMethod.equals("UPI") && !paymentMethod.equals("Credit Card") && !paymentMethod.equals("Debit Card") && !paymentMethod.equals("Online Banking"))
        {

            throw new RuntimeException("Invalid payment method");
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        booking.setPaymentId(paymentId);
        booking.setPaymentMode(paymentMethod);
        booking.setPaymentStatus(Booking.PaymentStatus.PAID);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        try
        {
            emailService.sendBookingConfirmation(savedBooking);

        }
        catch (Exception exception)
        {
            System.out.println("Booking email failed: " + exception.getMessage());
        }

        return savedBooking;
    }
}