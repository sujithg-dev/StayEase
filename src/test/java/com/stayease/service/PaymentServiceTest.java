
package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest
{
    @Mock BookingRepository bookingRepository;
    @Mock EmailService emailService;

    @InjectMocks PaymentService paymentService;

    private Booking booking;

    @BeforeEach
    void setUp()
    {
        booking = new Booking();
        booking.setBookingId("booking-1");
        booking.setCustomerId("customer-1");
        booking.setBookingStatus(Booking.BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
    }

    private void mockBooking()
    {
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void processPaymentConfirmsBookingForValidUpiPayment()
    {
        mockBooking();

        Booking result = paymentService.processPayment("booking-1", "customer-1", "UPI");

        assertEquals(Booking.BookingStatus.CONFIRMED, result.getBookingStatus());
        assertEquals(Booking.PaymentStatus.PAID, result.getPaymentStatus());
        assertEquals("UPI", result.getPaymentMode());
        assertNotNull(result.getPaymentId());
        assertTrue(result.getPaymentId().startsWith("PAY-"));
        verify(bookingRepository).save(booking);
        verify(emailService).sendBookingConfirmation(booking);
    }

    @Test
    void processPaymentSupportsCreditCard()
    {
        mockBooking();

        Booking result = paymentService.processPayment("booking-1", "customer-1", "Credit Card");

        assertEquals("Credit Card", result.getPaymentMode());
        assertEquals(Booking.PaymentStatus.PAID, result.getPaymentStatus());
    }

    @Test
    void processPaymentSupportsDebitCard()
    {
        mockBooking();

        Booking result = paymentService.processPayment("booking-1", "customer-1", "Debit Card");

        assertEquals("Debit Card", result.getPaymentMode());
    }

    @Test
    void processPaymentSupportsOnlineBanking()
    {
        mockBooking();

        Booking result = paymentService.processPayment("booking-1", "customer-1", "Online Banking");

        assertEquals("Online Banking", result.getPaymentMode());
    }

    @Test
    void processPaymentThrowsWhenBookingDoesNotExist()
    {
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-1", "UPI"));

        assertEquals("Booking not found", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsAnotherCustomersBooking()
    {
        mockBooking();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-2", "UPI"));

        assertEquals("You are not allowed to pay for this booking", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsCancelledBooking()
    {
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        mockBooking();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-1", "UPI"));

        assertEquals("Cancelled booking cannot be paid", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsBookingNotAwaitingPayment()
    {
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        mockBooking();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-1", "UPI"));

        assertEquals("This booking is not awaiting payment", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsAlreadyPaidBooking()
    {
        booking.setPaymentStatus(Booking.PaymentStatus.PAID);
        mockBooking();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-1", "UPI"));

        assertEquals("This booking has already been paid", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsInvalidPaymentMethod()
    {
        mockBooking();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment("booking-1", "customer-1", "Cash"));

        assertEquals("Invalid payment method", exception.getMessage());
        verify(bookingRepository).findById("booking-1");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPaymentDoesNotFailWhenConfirmationEmailThrows()
    {
        mockBooking();
        doThrow(new RuntimeException("Email server unavailable")).when(emailService).sendBookingConfirmation(any(Booking.class));

        Booking result = paymentService.processPayment("booking-1", "customer-1", "UPI");

        assertEquals(Booking.BookingStatus.CONFIRMED, result.getBookingStatus());
        assertEquals(Booking.PaymentStatus.PAID, result.getPaymentStatus());
        verify(bookingRepository).save(booking);
    }
}
