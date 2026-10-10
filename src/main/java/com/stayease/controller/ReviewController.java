package com.stayease.controller;

import com.stayease.dto.ReviewRequest;
import com.stayease.model.Review;
import com.stayease.model.User;
import com.stayease.service.ReviewService;
import com.stayease.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController
{
    private final UserService userService;
    private final ReviewService reviewService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping("/create")
    public String showReviewForm(@ModelAttribute("user") User user, @RequestParam String bookingId, Model model)
    {
        String customerId = user.getUserId();

        if (!reviewService.canReview(customerId, bookingId))
        {
            return "redirect:/bookings";
        }

        model.addAttribute("bookingId", bookingId);
        model.addAttribute("reviewRequest", new ReviewRequest());

        return "customer/review-form";
    }

    @PostMapping("/create")
    public String createReview(
            @ModelAttribute("user") User user,
            @RequestParam String bookingId,
            @Valid @ModelAttribute ReviewRequest reviewRequest,
            BindingResult bindingResult,
            Model model)
    {
        String customerId = user.getUserId();

        if (bindingResult.hasErrors())
        {
            model.addAttribute("bookingId", bookingId);

            return "customer/review-form";
        }

        Review review = reviewService.createReview(customerId, bookingId, reviewRequest);

        return "redirect:/hotels/" + review.getHotelId();
    }
}