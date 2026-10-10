package com.stayease.controller;

import com.stayease.dto.HotelSearchRequest;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.HotelService;
import com.stayease.service.ReviewService;
import com.stayease.service.RoomService;
import com.stayease.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController
{
    private final UserService userService;
    private final HotelService hotelService;
    private final RoomService roomService;
    private final ReviewService reviewService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping("/home")
    public String home(@ModelAttribute("searchRequest") HotelSearchRequest request)
    {
        return "customer/home";
    }

    @GetMapping("/hotels/search")
    public String searchHotels(
            @ModelAttribute("searchRequest") HotelSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "recommended") String sortBy,
            Model model)
    {
        List<Hotel> hotels = hotelService.getAllHotels();
        Page<Hotel> searchHotels = hotelService.searchHotels(request, page, size, sortBy);

        model.addAttribute("amenities", Hotel.HotelAmenity.values());
        model.addAttribute("hotels", hotels);
        model.addAttribute("searchHotels", searchHotels.getContent());
        model.addAttribute("totalPages", searchHotels.getTotalPages());
        model.addAttribute("totalHotels", searchHotels.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);

        return "customer/hotels";
    }

    @GetMapping("/hotels/{hotelId}")
    public String hotelDetails(@PathVariable String hotelId, Model model)
    {
        Hotel hotel = hotelService.getHotelById(hotelId);

        if (hotel == null)
        {
            return "redirect:/customer/dashboard";
        }

        if (hotel.getHotelStatus() != Hotel.HotelStatus.ACTIVE)
        {
            return "redirect:/customer/dashboard";
        }

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", roomService.getAvailableHotelRooms(hotelId));
        model.addAttribute("reviews", reviewService.getHotelReviews(hotelId));

        return "customer/hotel-details";
    }
}