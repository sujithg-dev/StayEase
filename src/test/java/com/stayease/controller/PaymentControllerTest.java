
package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.model.Booking;
import com.stayease.model.User;
import com.stayease.service.PaymentService;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PaymentControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PaymentService paymentService;

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
    void showPaymentPageReturnsPaymentView() throws Exception
    {
        mockMvc.perform(get("/payment/create")
                        .param("bookingId", "booking-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/payment"))
                .andExpect(model().attribute("bookingId", "booking-1"))
                .andExpect(model().attributeExists("paymentRequest"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void processPaymentRedirectsToBookingDetailsWhenSuccessful() throws Exception
    {
        User user = mockUser();
        Booking booking = new Booking();
        booking.setBookingId("booking-1");

        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(user);
        when(paymentService.processPayment(eq("booking-1"), eq("customer-1"), any())).thenReturn(booking);

        mockMvc.perform(post("/payment/create")
                        .with(csrf())
                        .param("bookingId", "booking-1")
                        .param("paymentMethod", "UPI"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/booking-1"));

        verify(paymentService).processPayment(eq("booking-1"), eq("customer-1"), any());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void processPaymentReturnsPaymentViewWhenValidationFails() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        mockMvc.perform(post("/payment/create")
                        .with(csrf())
                        .param("bookingId", "booking-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/payment"))
                .andExpect(model().attribute("bookingId", "booking-1"))
                .andExpect(model().attributeHasErrors("paymentRequest"));

        verify(paymentService, never()).processPayment(anyString(), anyString(), any());
    }
}
