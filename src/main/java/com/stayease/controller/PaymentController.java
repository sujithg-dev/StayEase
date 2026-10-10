package com.stayease.controller;

import com.stayease.dto.PaymentRequest;
import com.stayease.model.Booking;
import com.stayease.model.User;
import com.stayease.service.PaymentService;
import com.stayease.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController
{
    private final UserService userService;
    private final PaymentService paymentService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping("/create")
    public String showPaymentPage(@RequestParam String bookingId, Model model)
    {
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("paymentRequest", new PaymentRequest());

        return "customer/payment";
    }

    @PostMapping("/create")
    public String processPayment(
            @ModelAttribute("user") User user,
            @Valid @ModelAttribute PaymentRequest paymentRequest,
            @RequestParam String bookingId,
            BindingResult bindingResult,
            Model model)
    {
        String customerId = user.getUserId();

        if (bindingResult.hasErrors())
        {
            model.addAttribute("bookingId", bookingId);

            return "customer/payment";
        }

        Booking booking = paymentService.processPayment(bookingId, customerId, paymentRequest.getPaymentMethod());

        return "redirect:/bookings/" + booking.getBookingId();
    }
}