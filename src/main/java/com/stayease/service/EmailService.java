package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService
{
    private final RestClient restClient;
    private final String fromEmail;
    private final String fromName;
    private final String baseUrl;

    public EmailService(@Value("${brevo.api-key}") String apiKey,
                        @Value("${mail.from}") String fromEmail,
                        @Value("${mail.from-name:StayEase}") String fromName,
                        @Value("${app.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", "application/json")
                .build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
    }

    public void sendBookingConfirmation(Booking booking)
    {
        String subject = "StayEase Booking Confirmed - " + booking.getBookingNumber();

        String text = """
                Hello %s,

                Your StayEase booking has been confirmed.

                Booking Number: %s
                Hotel: %s
                Room: %s
                Check-in: %s
                Check-out: %s
                Guests: %s
                Rooms: %s
                Total Amount: ₹%s
                Payment ID: %s

                Thank you for choosing StayEase!
                """.formatted(
                booking.getCustomerName(),
                booking.getBookingNumber(),
                booking.getHotelName(),
                booking.getRoomType(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalGuests(),
                booking.getTotalRooms(),
                booking.getTotalAmount(),
                booking.getPaymentId());

        send(booking.getCustomerEmail(), booking.getCustomerName(), subject, text);
    }

    public void sendBookingCancellation(Booking booking)
    {
        String subject = "StayEase Booking Cancelled - " + booking.getBookingNumber();

        String text = """
                Hello %s,

                Your StayEase booking has been cancelled.

                Booking Number: %s
                Hotel: %s
                Check-in: %s
                Check-out: %s

                If you have any questions, please contact StayEase support.
                """.formatted(
                booking.getCustomerName(),
                booking.getBookingNumber(),
                booking.getHotelName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());

        send(booking.getCustomerEmail(), booking.getCustomerName(), subject, text);
    }

    public void sendPasswordResetEmail(User user, String rawToken)
    {
        String resetLink = baseUrl + "/reset-password?token=" + rawToken;

        String text = """
                Hello %s,

                We received a request to reset your StayEase password.

                Click the link below to reset your password:

                %s

                This link will expire in 30 minutes.

                If you did not request a password reset, you can safely ignore this email.

                StayEase Team
                """.formatted(user.getUserName(), resetLink);

        send(user.getUserEmail(), user.getUserName(), "StayEase - Reset Your Password", text);
    }

    private void send(String toEmail, String toName, String subject, String textContent)
    {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", toEmail, "name", toName != null ? toName : toEmail)),
                "subject", subject,
                "textContent", textContent);

        try
        {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        }
        catch (RestClientException e)
        {
            log.error("Failed to send email '{}' to {}: {}", subject, toEmail, e.getMessage());
        }
    }
}