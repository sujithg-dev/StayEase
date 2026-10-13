package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.AdminService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AdminControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AdminService adminService;

    private User admin;

    @BeforeEach
    void setUp()
    {
        admin = new User();
        admin.setUserId("admin-1");
        admin.setUserName("Test Admin");
        admin.setUserEmail("admin@example.com");

        lenient().when(userService.getUserByUserEmail("admin@example.com")).thenReturn(admin);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void dashboard_returnsDashboardWithStatistics() throws Exception
    {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists(
                        "customerCount",
                        "managerCount",
                        "activeHotelCount",
                        "inactiveHotelCount",
                        "confirmedBookingCount",
                        "pendingPaymentCount",
                        "cancelledBookingCount",
                        "completedBookingCount",
                        "userCount",
                        "hotelCount",
                        "bookingCount",
                        "currentPage",
                        "user"
                ));

        verify(adminService).getCustomerCount();
        verify(adminService).getManagerCount();
        verify(adminService).getActiveHotelCount();
        verify(adminService).getInactiveHotelCount();
        verify(adminService).getConfirmedBookingCount();
        verify(adminService).getPendingPaymentCount();
        verify(adminService).getCancelledBookingCount();
        verify(adminService).getCompletedBookingCount();
        verify(adminService).getUserCount();
        verify(adminService).getHotelCount();
        verify(adminService).getBookingCount();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void users_returnsUsersPage() throws Exception
    {
        when(adminService.getAllUsers()).thenReturn(List.of(admin));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users", "adminId", "currentPage", "user"))
                .andExpect(model().attribute("adminId", "admin-1"));

        verify(adminService).getAllUsers();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void hotels_returnsHotelsAndManagerDetails() throws Exception
    {
        Hotel hotel = new Hotel();
        hotel.setManagerId("manager-1");

        User manager = new User();
        manager.setUserId("manager-1");
        manager.setUserName("Test Manager");
        manager.setUserEmail("manager@example.com");

        when(adminService.getAllHotels()).thenReturn(List.of(hotel));
        when(userService.getUserByUserId("manager-1")).thenReturn(Optional.of(manager));

        mockMvc.perform(get("/admin/hotels"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/hotels"))
                .andExpect(model().attributeExists("hotels", "managerNames", "managerEmails", "currentPage", "user"));

        verify(adminService).getAllHotels();
        verify(userService).getUserByUserId("manager-1");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void hotels_whenManagerNotFound_returnsHotelsPage() throws Exception
    {
        Hotel hotel = new Hotel();
        hotel.setManagerId("missing-manager");

        when(adminService.getAllHotels()).thenReturn(List.of(hotel));
        when(userService.getUserByUserId("missing-manager")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/hotels"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/hotels"))
                .andExpect(model().attributeExists("managerNames", "managerEmails", "hotels"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void bookings_returnsBookingsPage() throws Exception
    {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings"))
                .andExpect(model().attributeExists("bookings", "currentPage", "user"));

        verify(adminService).getAllBookings();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void changeHotelStatus_changesStatusAndRedirects() throws Exception
    {
        mockMvc.perform(post("/admin/hotels/status/hotel-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hotels"));

        verify(adminService).changeHotelStatus("hotel-1");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void changeUserStatus_changesStatusAndRedirects() throws Exception
    {
        mockMvc.perform(post("/admin/users/status/user-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminService).changeUserStatus("user-1");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void changeUserRole_changesRoleAndRedirects() throws Exception
    {
        mockMvc.perform(post("/admin/users/role/user-1")
                        .with(csrf())
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminService).changeUserRole("user-1", User.UserRole.MANAGER, "admin-1");
    }
}