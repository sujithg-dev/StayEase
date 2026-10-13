package com.stayease.controller;

import com.stayease.helper.TablePaging;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.AdminService;
import com.stayease.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController
{
    private final UserService userService;
    private final AdminService adminService;

    private static final Map<String, String> USER_SORTABLE = Map.of(
            "userName",      "userName",
            "userEmail",     "userEmail",
            "userRole",      "userRole",
            "userStatus",    "userStatus");

    private static final Map<String, String> HOTEL_SORTABLE = Map.of(
            "hotelName",     "hotelName",
            "managerName",   "managerName",
            "managerEmail",  "managerEmail",
            "hotelRating",   "hotelRating",
            "hotelStatus",   "hotelStatus",
            "hotelCity",     "hotelCity",
            "hotelState",    "hotelState",
            "hotelCountry",  "hotelCountry");

    private static final Map<String, String> BOOKING_SORTABLE = Map.of(
            "bookingNumber", "bookingNumber",
            "customerName",  "customerName",
            "hotelName",     "hotelName",
            "roomType",      "roomType",
            "checkInDate",   "checkInDate",
            "checkOutDate",  "checkOutDate",
            "totalRooms",    "totalRooms",
            "totalAmount",   "totalAmount",
            "paymentStatus", "paymentStatus",
            "bookingStatus", "bookingStatus");

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
    public String users(
            @ModelAttribute("user") User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model)
    {
        Pageable pageable = TablePaging.of(page, size, sort, dir, USER_SORTABLE, "id");
        Page<User> result = adminService.getUsers(q, role, status, pageable);

        boolean hasFilters = Stream.of(q, role, status).anyMatch(s -> s != null && !s.isBlank());

        String adminId = user.getUserId();

        model.addAttribute("currentPage", "adminUsers");
        model.addAttribute("page", result);
        model.addAttribute("allUsers", adminService.getAllUsers());
        model.addAttribute("users", result.getContent());
        model.addAttribute("hasFilters", hasFilters);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("userRoles", Arrays.stream(User.UserRole.values()).filter(r -> r != User.UserRole.ADMIN).toList());
        model.addAttribute("userStatuses", User.UserStatus.values());
        model.addAttribute("adminId", adminId);

        return "admin/users";
    }

    @GetMapping("/hotels")
    public String hotels(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model)
    {
        Pageable pageable = TablePaging.of(page, size, sort, dir, HOTEL_SORTABLE, "id");
        Page<Hotel> result = adminService.getHotels(q, status, pageable);

        boolean hasFilters = Stream.of(q, status).anyMatch(s -> s != null && !s.isBlank());

        model.addAttribute("currentPage", "adminHotels");
        model.addAttribute("page", result);
        model.addAttribute("allHotels", adminService.getAllHotels());
        model.addAttribute("hotels", result.getContent());
        model.addAttribute("hasFilters", hasFilters);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("hotelStatuses", Hotel.HotelStatus.values());

        return "admin/hotels";
    }

    @GetMapping("/bookings")
    public String bookings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String hotel,
            @RequestParam(required = false) String payment,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model)
    {
        Pageable pageable = TablePaging.of(page, size, sort, dir, BOOKING_SORTABLE, "id");
        Page<Booking> result = adminService.getBookings(q, hotel, payment, status, from, to, pageable);

        boolean hasFilters = Stream.of(q, hotel, payment, status).anyMatch(s -> s != null && !s.isBlank())
                || from != null || to != null;

        model.addAttribute("currentPage", "adminBookings");
        model.addAttribute("page", result);
        model.addAttribute("allBookings", adminService.getAllBookings());
        model.addAttribute("bookings", result.getContent());
        model.addAttribute("hasFilters", hasFilters);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("q", q);
        model.addAttribute("hotel", hotel);
        model.addAttribute("payment", payment);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("paymentStatuses", Booking.PaymentStatus.values());
        model.addAttribute("bookingStatuses", Booking.BookingStatus.values());
        model.addAttribute("hotels", adminService.getBookingHotelNames());

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
    public String changeUserRole(@PathVariable String userId, @RequestParam User.UserRole role, Authentication authentication)
    {
        User admin = userService.getUserByUserEmail(authentication.getName());
        adminService.changeUserRole(userId, role, admin.getUserId());
        return "redirect:/admin/users";
    }
}
