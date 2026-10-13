package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest
{
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Booking booking;
    private User user;

    @BeforeEach
    void setUp()
    {
        booking = new Booking();
        booking.setCustomerEmail("customer@example.com");
        booking.setCustomerName("Test Customer");
        booking.setBookingNumber("SE-12345678");
        booking.setHotelName("StayEase Hotel");
        booking.setRoomType("DELUXE");
        booking.setCheckInDate(LocalDate.of(2026, 11, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 11, 12));
        booking.setTotalGuests(2);
        booking.setTotalRooms(1);
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setPaymentId("PAY-12345");

        user = new User();
        user.setUserEmail("customer@example.com");
        user.setUserName("Test Customer");
    }

    private SimpleMailMessage captureSentMessage()
    {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendBookingConfirmation_sendsCorrectEmail()
    {
        emailService.sendBookingConfirmation(booking);

        SimpleMailMessage message = captureSentMessage();

        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("StayEase Booking Confirmed - SE-12345678", message.getSubject());

        String text = message.getText();
        assertNotNull(text);
        assertTrue(text.contains("Hello Test Customer"));
        assertTrue(text.contains("Your StayEase booking has been confirmed."));
        assertTrue(text.contains("Booking Number: SE-12345678"));
        assertTrue(text.contains("Hotel: StayEase Hotel"));
        assertTrue(text.contains("Room: DELUXE"));
        assertTrue(text.contains("Check-in: 2026-11-10"));
        assertTrue(text.contains("Check-out: 2026-11-12"));
        assertTrue(text.contains("Guests: 2"));
        assertTrue(text.contains("Rooms: 1"));
        assertTrue(text.contains("Total Amount: ₹5000.00"));
        assertTrue(text.contains("Payment ID: PAY-12345"));
        assertTrue(text.contains("Thank you for choosing StayEase!"));
    }

    @Test
    void sendBookingCancellation_sendsCorrectEmail()
    {
        emailService.sendBookingCancellation(booking);

        SimpleMailMessage message = captureSentMessage();

        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("StayEase Booking Cancelled - SE-12345678", message.getSubject());

        String text = message.getText();
        assertNotNull(text);
        assertTrue(text.contains("Hello Test Customer"));
        assertTrue(text.contains("Your StayEase booking has been cancelled."));
        assertTrue(text.contains("Booking Number: SE-12345678"));
        assertTrue(text.contains("Hotel: StayEase Hotel"));
        assertTrue(text.contains("Check-in: 2026-11-10"));
        assertTrue(text.contains("Check-out: 2026-11-12"));
        assertTrue(text.contains("please contact StayEase support."));
    }

    @Test
    void sendPasswordResetEmail_sendsCorrectEmail()
    {
        String rawToken = "test-reset-token";

        emailService.sendPasswordResetEmail(user, rawToken);

        SimpleMailMessage message = captureSentMessage();

        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("StayEase - Reset Your Password", message.getSubject());

        String text = message.getText();
        assertNotNull(text);
        assertTrue(text.contains("Hello Test Customer"));
        assertTrue(text.contains("We received a request to reset your StayEase password."));
        assertTrue(text.contains("http://localhost:8080/reset-password?token=" + rawToken));
        assertTrue(text.contains("This link will expire in 30 minutes."));
        assertTrue(text.contains("If you did not request a password reset"));
        assertTrue(text.contains("StayEase Team"));
    }

    @Test
    void sendBookingConfirmation_whenMailSenderFails_propagatesException()
    {
        doThrow(new RuntimeException("Mail server unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> emailService.sendBookingConfirmation(booking));

        assertEquals("Mail server unavailable", exception.getMessage());
    }

    @Test
    void sendBookingCancellation_whenMailSenderFails_propagatesException()
    {
        doThrow(new RuntimeException("Mail server unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> emailService.sendBookingCancellation(booking));

        assertEquals("Mail server unavailable", exception.getMessage());
    }

    @Test
    void sendPasswordResetEmail_whenMailSenderFails_propagatesException()
    {
        doThrow(new RuntimeException("Mail server unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> emailService.sendPasswordResetEmail(user, "test-token"));

        assertEquals("Mail server unavailable", exception.getMessage());
    }
}