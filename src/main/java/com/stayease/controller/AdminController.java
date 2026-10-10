package com.stayease.controller;

import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.AdminService;
import com.stayease.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController
{
    private final UserService userService;
    private final AdminService adminService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model)
    {
        model.addAttribute("customerCount", adminService.getCustomerCount());
        model.addAttribute("managerCount", adminService.getManagerCount());
        model.addAttribute("activeHotelCount", adminService.getActiveHotelCount());
        model.addAttribute("inactiveHotelCount", adminService.getInactiveHotelCount());
        model.addAttribute("confirmedBookingCount", adminService.getConfirmedBookingCount());
        model.addAttribute("pendingPaymentCount", adminService.getPendingPaymentCount());
        model.addAttribute("cancelledBookingCount", adminService.getCancelledBookingCount());
        model.addAttribute("completedBookingCount", adminService.getCompletedBookingCount());
        model.addAttribute("userCount", adminService.getUserCount());
        model.addAttribute("hotelCount", adminService.getHotelCount());
        model.addAttribute("bookingCount", adminService.getBookingCount());
        model.addAttribute("currentPage", "adminDashboard");

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@ModelAttribute("user") User user, Model model)
    {
        String adminId = user.getUserId();

        model.addAttribute("currentPage", "adminUsers");
        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("adminId", adminId);

        return "admin/users";
    }

    @GetMapping("/hotels")
    public String hotels(Model model)
    {
        List<Hotel> hotels = adminService.getAllHotels();

        Map<String, String> managerNames = new HashMap<>();
        Map<String, String> managerEmails = new HashMap<>();

        for (Hotel hotel : hotels)
        {
            userService.getUserByUserId(hotel.getManagerId()).ifPresent(manager ->
            {
                managerNames.put(hotel.getManagerId(), manager.getUserName());
                managerEmails.put(hotel.getManagerId(), manager.getUserEmail());
            });
        }

        model.addAttribute("currentPage", "adminHotels");
        model.addAttribute("managerNames", managerNames);
        model.addAttribute("managerEmails", managerEmails);
        model.addAttribute("hotels", hotels);

        return "admin/hotels";
    }

    @GetMapping("/bookings")
    public String bookings(Model model)
    {
        model.addAttribute("currentPage", "adminBookings");
        model.addAttribute("bookings", adminService.getAllBookings());

        return "admin/bookings";
    }

    @PostMapping("/hotels/status/{id}")
    public String changeHotelStatus(@PathVariable String id)
    {
        adminService.changeHotelStatus(id);

        return "redirect:/admin/hotels";
    }

    @PostMapping("/users/status/{id}")
    public String changeUserStatus(@PathVariable String id)
    {
        adminService.changeUserStatus(id);

        return "redirect:/admin/users";
    }

    @PostMapping("/users/role/{userId}")
    public String changeUserRole(@ModelAttribute("user") User user, @PathVariable String userId, @RequestParam User.UserRole role, Authentication authentication)
    {
        User admin = userService.getUserByUserEmail(authentication.getName());

        adminService.changeUserRole(userId, role, admin.getUserId());

        return "redirect:/admin/users";
    }
}
