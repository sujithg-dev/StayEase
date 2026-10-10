package com.stayease.controller;

import com.stayease.dto.BookingRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.service.BookingService;
import com.stayease.service.HotelService;
import com.stayease.service.RoomService;
import com.stayease.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController
{
    private final UserService userService;
    private final HotelService hotelService;
    private final RoomService roomService;
    private final BookingService bookingService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping
    public String allBookings(@ModelAttribute("user") User user, Model model)
    {
        String customerId = user.getUserId();

        LocalDate today = LocalDate.now();

        List<Booking> upcomingBookings = bookingService.getCustomerBookings(customerId).stream()
                .filter(booking -> (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED || booking.getBookingStatus() == Booking.BookingStatus.PENDING_PAYMENT) && !booking.getCheckInDate().isBefore(today))
                .sorted(Comparator.comparing(Booking::getCheckInDate))
                .toList();

        List<Booking> otherBookings = bookingService.getCustomerBookings(customerId)
                .stream()
                .filter(booking -> !upcomingBookings.contains(booking))
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("otherBookings", otherBookings);

        return "customer/bookings";
    }

    @GetMapping("/{bookingId}")
    public String bookingDetails(@ModelAttribute("user") User user, @PathVariable String bookingId, Model model)
    {
        String customerId = user.getUserId();

        Booking booking = bookingService.getCustomerBooking(bookingId, customerId);

        model.addAttribute("booking", booking);

        return "customer/booking-confirmation";
    }

    @GetMapping("/create")
    public String showBookingForm(@RequestParam String hotelId, @RequestParam String roomId, Model model)
    {
        Hotel hotel = hotelService.getHotelById(hotelId);
        Room room = roomService.getRoomById(roomId);

        if (!room.getHotelId().equals(hotelId))
        {
            return "redirect:/customer/hotels";
        }

        if (hotel.getHotelStatus() != Hotel.HotelStatus.ACTIVE)
        {
            return "redirect:/customer/hotels";
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("bookingRequest", new BookingRequest());

        return "customer/booking-form";
    }

    @PostMapping("/create")
    public String createBooking(
            @ModelAttribute("user") User user,
            @RequestParam String hotelId,
            @RequestParam String roomId,
            @Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
            BindingResult bindingResult,
            Model model)
    {
        Hotel hotel = hotelService.getHotelById(hotelId);
        Room room = roomService.getRoomById(roomId);

        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);

        if (bindingResult.hasErrors())
        {
            return "customer/booking-form";
        }

        try
        {
            Booking booking = bookingService.createBooking(bookingRequest, hotelId, roomId, user);

            return "redirect:/payment/create?bookingId=" + booking.getBookingId();
        }
        catch (RuntimeException e)
        {
            model.addAttribute("error", e.getMessage());

            return "customer/booking-form";
        }
    }

    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@ModelAttribute("user") User user, @PathVariable String bookingId)
    {
        String customerId = user.getUserId();

        try
        {
            bookingService.cancelBooking(bookingId, customerId);
        }
        catch (RuntimeException ignored) {}

        return "redirect:/bookings";
    }
}