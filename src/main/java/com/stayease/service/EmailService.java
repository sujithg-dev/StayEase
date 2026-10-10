package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService
{
    private final JavaMailSender mailSender;

    public void sendBookingConfirmation(Booking booking)
    {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(booking.getCustomerEmail());
        message.setSubject("StayEase Booking Confirmed - " + booking.getBookingNumber());

        message.setText("Hello " + booking.getCustomerName() + ",\n\n"
                        + "Your StayEase booking has been confirmed.\n\n"
                        + "Booking Number: "
                        + booking.getBookingNumber() + "\n"
                        + "Hotel: "
                        + booking.getHotelName() + "\n"
                        + "Room: "
                        + booking.getRoomType() + "\n"
                        + "Check-in: "
                        + booking.getCheckInDate() + "\n"
                        + "Check-out: "
                        + booking.getCheckOutDate() + "\n"
                        + "Guests: "
                        + booking.getTotalGuests() + "\n"
                        + "Rooms: "
                        + booking.getTotalRooms() + "\n"
                        + "Total Amount: ₹"
                        + booking.getTotalAmount() + "\n"
                        + "Payment ID: "
                        + booking.getPaymentId() + "\n\n"
                        + "Thank you for choosing StayEase!"
        );

        mailSender.send(message);
    }

    public void sendBookingCancellation(Booking booking)
    {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(booking.getCustomerEmail());

        message.setSubject("StayEase Booking Cancelled - " + booking.getBookingNumber());

        message.setText("Hello " + booking.getCustomerName() + ",\n\n"
                        + "Your StayEase booking has been cancelled.\n\n"
                        + "Booking Number: "
                        + booking.getBookingNumber() + "\n"
                        + "Hotel: "
                        + booking.getHotelName() + "\n"
                        + "Check-in: "
                        + booking.getCheckInDate() + "\n"
                        + "Check-out: "
                        + booking.getCheckOutDate() + "\n\n"
                        + "If you have any questions, "
                        + "please contact StayEase support."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(User user, String rawToken)
    {
        String resetLink = "http://localhost:8080/reset-password?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(user.getUserEmail());

        message.setSubject("StayEase - Reset Your Password");

        message.setText("Hello " + user.getUserName() + ",\n\n"
                        + "We received a request to reset your "
                        + "StayEase password.\n\n"
                        + "Click the link below to reset your password:\n\n"
                        + resetLink + "\n\n"
                        + "This link will expire in 30 minutes.\n\n"
                        + "If you did not request a password reset, "
                        + "you can safely ignore this email.\n\n"
                        + "StayEase Team"
        );

        mailSender.send(message);
    }
}