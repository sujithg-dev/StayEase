package com.stayease.controller;

import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController
{
    private final UserService userService;
    private final ManagerService managerService;
    private final HotelService hotelService;
    private final BookingService bookingService;
    private final ImageService imageService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("user") User user, Model model)
    {
        String managerId = user.getUserId();

        model.addAttribute("currentPage", "managerDashboard");
        model.addAttribute("hotelCount", managerService.getHotelCountByManageId(managerId));
        model.addAttribute("activeHotelCount", managerService.getActiveHotelCountByManageId(managerId));
        model.addAttribute("inactiveHotelCount", managerService.getInactiveHotelCountByManageId(managerId));
        model.addAttribute("bookingCount", managerService.getBookingCountByManageId(managerId));
        model.addAttribute("confirmedBookingCount", managerService.getConfirmedBookingCountByManageId(managerId));
        model.addAttribute("pendingPaymentCount", managerService.getPendingPaymentCountByManageId(managerId));
        model.addAttribute("cancelledBookingCount", managerService.getCancelledBookingCountByManageId(managerId));
        model.addAttribute("completedBookingCount", managerService.getCompletedBookingCountByManageId(managerId));

        return "manager/dashboard";
    }

    @GetMapping("/hotels")
    public String allManagerhotels(@ModelAttribute("user") User user, Model model)
    {
        String managerId = user.getUserId();

        model.addAttribute("currentPage", "managerHotels");
        model.addAttribute("hotels", managerService.getManagerHotelsByManageId(managerId));

        return "manager/hotels";
    }

    @GetMapping("/bookings")
    public String allManagerHotelBookings(@ModelAttribute("user") User user, Model model)
    {
        String managerId = user.getUserId();

        List<Booking> bookings = managerService.getManagerHotelsByManageId(managerId)
                .stream()
                .flatMap(hotel -> bookingService.getHotelBookings(hotel.getHotelId()).stream())
                .toList();

        model.addAttribute("currentPage", "managerBookings");
        model.addAttribute("manager", managerId);
        model.addAttribute("bookings", bookings);

        return "manager/bookings";
    }

    @GetMapping("/bookings/{bookingId}")
    public String showManagerHotelbookingDetails(@ModelAttribute("user") User user, @PathVariable String bookingId, Model model)
    {
        String managerId = user.getUserId();

        Booking booking = bookingService.getBookingById(bookingId);

        if (!managerService.isHotelOwnedByManager(booking.getHotelId(), managerId))
        {
            return "redirect:/manager/bookings";
        }

        model.addAttribute("currentPage", "managerBookings");
        model.addAttribute("booking", booking);

        return "manager/booking-details";
    }

    @GetMapping("/hotels/add")
    public String showAddHotelPage(Model model)
    {
        model.addAttribute("currentPage", "managerHotels");
        model.addAttribute("amenities", Hotel.HotelAmenity.values());
        model.addAttribute("hotel", new Hotel());

        return "manager/add-hotel";
    }

    @GetMapping("/hotels/edit/{hotelId}")
    public String showEditHotelPage(@ModelAttribute("user") User user, @PathVariable String hotelId, Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            throw new RuntimeException("You are not allowed to edit this hotel");
        }

        Hotel hotel = hotelService.getHotelById(hotelId);

        model.addAttribute("currentPage", "managerHotels");
        model.addAttribute("amenities", Hotel.HotelAmenity.values());
        model.addAttribute("hotel", hotel);

        return "manager/edit-hotel";
    }

    @PostMapping("/hotels/add")
    public String addHotel(
            @ModelAttribute("user") User user,
            @Valid @ModelAttribute("hotel") Hotel hotel,
            BindingResult bindingResult,
            @RequestParam(value = "hotelPolicies", required = false) String policiesText,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            Model model)
    {
        String managerId = user.getUserId();

        if (bindingResult.hasErrors())
        {
            model.addAttribute("currentPage", "managerHotels");
            model.addAttribute("amenities", Hotel.HotelAmenity.values());

            return "manager/add-hotel";
        }

        List<String> imagePaths = imageService.saveHotelImages(imageFiles);

        hotel.setHotelPolicies(parsePolicies(policiesText));
        hotel.setHotelImages(imagePaths);
        hotel.setHotelCoverImage(imagePaths.isEmpty() ? null : imagePaths.getFirst());
        hotel.setManagerId(managerId);
        hotel.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        hotelService.createHotel(hotel, managerId);

        return "redirect:/manager/hotels";
    }

    @PostMapping("/hotels/edit/{hotelId}")
    public String updateHotel(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @Valid @ModelAttribute("hotel") Hotel hotel,
            BindingResult bindingResult,
            @RequestParam(value = "hotelPolicies", required = false) String policiesText,
            Model model)
    {
        String managerId = user.getUserId();

        if (!managerService.isHotelOwnedByManager(hotelId, managerId))
        {
            return "redirect:/manager/hotels";
        }

        if (bindingResult.hasErrors())
        {
            model.addAttribute("currentPage", "managerHotels");
            model.addAttribute("hotelId", hotelId);
            model.addAttribute("amenities", Hotel.HotelAmenity.values());

            return "manager/edit-hotel";
        }

        hotel.setHotelPolicies(parsePolicies(policiesText));
        hotelService.updateHotel(hotelId, hotel, managerId);

        return "redirect:/manager/hotels";
    }

    @PostMapping("/hotels/delete/{hotelId}")
    public String deleteHotel(@ModelAttribute("user") User user, @PathVariable String hotelId)
    {
        String managerId = user.getUserId();

        hotelService.deleteHotel(hotelId, managerId);

        return "redirect:/manager/hotels";
    }

    @PostMapping("/hotels/edit/image/{hotelId}")
    public String updateHotelImage(
            @ModelAttribute("user") User user,
            @ModelAttribute Hotel hotel,
            @PathVariable String hotelId,
            BindingResult bindingResult,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles)
    {
        String managerId = user.getUserId();

        if (bindingResult.hasErrors())
        {
            return "manager/edit-hotel";
        }

        Hotel existingHotel = hotelService.getHotelById(hotelId);

        if (existingHotel == null)
        {
            return "redirect:/manager/hotels";
        }

        List<String> images = new ArrayList<>();

        if (existingHotel.getHotelImages() != null)
        {
            images.addAll(existingHotel.getHotelImages());
        }

        List<String> newImages = imageService.saveHotelImages(imageFiles);
        images.addAll(newImages);

        hotel.setHotelImages(images);
        hotel.setHotelCoverImage(existingHotel.getHotelCoverImage());

        hotelService.addHotelImage(hotelId, hotel, managerId);

        return "redirect:/manager/hotels/edit/" + hotelId;
    }

    @PostMapping("/hotels/{hotelId}/images/cover")
    public String setHotelCoverImage(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @RequestParam String image)
    {
        String managerId = user.getUserId();

        hotelService.setHotelCoverImage(hotelId, image, managerId);
        return "redirect:/manager/hotels/edit/" + hotelId;
    }

    @PostMapping("/hotels/{hotelId}/images/delete")
    public String deleteHotelImage(
            @ModelAttribute("user") User user,
            @PathVariable String hotelId,
            @RequestParam String image)
    {
        String managerId = user.getUserId();

        hotelService.deleteHotelImage(hotelId, image, managerId);

        return "redirect:/manager/hotels/edit/" + hotelId;
    }

    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking( @ModelAttribute("user") User user, @PathVariable String bookingId)
    {
        String managerId = user.getUserId();

        Booking booking = bookingService.getBookingById(bookingId);

        if (managerService.isHotelOwnedByManager(booking.getHotelId(), managerId))
        {
            bookingService.cancelBookingByManager(bookingId, booking.getHotelId());
        }

        return "redirect:/manager/bookings";
    }

    private List<String> parsePolicies(String policiesText)
    {
        if (policiesText == null || policiesText.isBlank())
        {
            return new ArrayList<>();
        }

        return Arrays.stream(policiesText.split("\\R"))
                .map(String::trim)
                .filter(policy -> !policy.isEmpty())
                .toList();
    }
}