package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.dto.ReviewRequest;
import com.stayease.model.Review;
import com.stayease.model.User;
import com.stayease.service.ReviewService;
import com.stayease.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ReviewControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReviewService reviewService;

    private User mockUser()
    {
        User user = new User();
        user.setUserId("customer-1");
        user.setUserName("Test Customer");
        user.setUserEmail("customer@example.com");
        return user;
    }

    @BeforeEach
    void setUp()
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void showReviewFormReturnsViewWhenCustomerCanReview() throws Exception
    {
        when(reviewService.canReview("customer-1", "booking-1")).thenReturn(true);

        mockMvc.perform(get("/review/create")
                        .param("bookingId", "booking-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/review-form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("bookingId"))
                .andExpect(model().attributeExists("reviewRequest"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void showReviewFormRedirectsWhenCustomerCannotReview() throws Exception
    {
        when(reviewService.canReview("customer-1", "booking-1")).thenReturn(false);

        mockMvc.perform(get("/review/create")
                        .param("bookingId", "booking-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void createReviewRedirectsToHotelWhenSuccessful() throws Exception
    {
        Review review = new Review();
        review.setHotelId("hotel-1");

        when(reviewService.createReview(eq("customer-1"), eq("booking-1"), any(ReviewRequest.class))).thenReturn(review);

        mockMvc.perform(post("/review/create")
                        .param("bookingId", "booking-1")
                        .param("rating", "5")
                        .param("comment", "Excellent stay")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hotels/hotel-1"));

        verify(reviewService).createReview(eq("customer-1"), eq("booking-1"), any(ReviewRequest.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void createReviewReturnsFormWhenValidationFails() throws Exception
    {
        mockMvc.perform(post("/review/create")
                        .param("bookingId", "booking-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/review-form"))
                .andExpect(model().attributeExists("bookingId"))
                .andExpect(model().attributeHasErrors("reviewRequest"));

        verify(reviewService, never()).createReview(anyString(), anyString(), any(ReviewRequest.class));
    }
}