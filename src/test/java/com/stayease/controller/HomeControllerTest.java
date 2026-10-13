package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.dto.HotelSearchRequest;
import com.stayease.service.HotelService;
import com.stayease.service.ReviewService;
import com.stayease.service.RoomService;
import com.stayease.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class HomeControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private HotelService hotelService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private ReviewService reviewService;

    private User mockUser()
    {
        User user = new User();
        user.setUserId("customer-1");
        user.setUserName("Customer");
        user.setUserEmail("customer@example.com");
        return user;
    }

    private Hotel mockHotel(Hotel.HotelStatus status)
    {
        Hotel hotel = new Hotel();
        hotel.setHotelId("hotel-1");
        hotel.setHotelName("Coastal Pearl Resort");
        hotel.setHotelStatus(status);
        return hotel;
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void homeReturnsCustomerHomePage() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        mockMvc.perform(get("/home")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/home"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("searchRequest"));

        verify(userService).getUserByUserEmail("customer@example.com");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void searchHotelsReturnsHotelSearchPage() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        Hotel hotel = mockHotel(Hotel.HotelStatus.ACTIVE);

        when(hotelService.getAllHotels()).thenReturn(List.of(hotel));
        when(hotelService.searchHotels(any(HotelSearchRequest.class), eq(0), eq(6), eq("recommended"))).thenReturn(new PageImpl<>(List.of(hotel), PageRequest.of(0, 6), 1));

        mockMvc.perform(get("/hotels/search")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/hotels"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("amenities"))
                .andExpect(model().attributeExists("hotels"))
                .andExpect(model().attributeExists("searchHotels"))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalHotels", 1L))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("size", 6))
                .andExpect(model().attribute("sortBy", "recommended"));

        verify(hotelService).getAllHotels();
        verify(hotelService).searchHotels(any(HotelSearchRequest.class), eq(0), eq(6), eq("recommended"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void searchHotelsUsesRequestedPaginationAndSorting() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getAllHotels()).thenReturn(List.of());
        when(hotelService.searchHotels(any(HotelSearchRequest.class), eq(1), eq(3), eq("priceAsc"))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 3), 0));

        mockMvc.perform(get("/hotels/search")
                        .with(csrf())
                        .param("page", "1")
                        .param("size", "3")
                        .param("sortBy", "priceAsc"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/hotels"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("size", 3))
                .andExpect(model().attribute("sortBy", "priceAsc"))
                .andExpect(model().attribute("totalHotels", 0L));

        verify(hotelService).searchHotels(any(HotelSearchRequest.class), eq(1), eq(3), eq("priceAsc"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void hotelDetailsReturnsDetailsForActiveHotel() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        Hotel hotel = mockHotel(Hotel.HotelStatus.ACTIVE);
        List<Room> rooms = List.of(new Room());

        when(hotelService.getHotelById("hotel-1")).thenReturn(hotel);
        when(roomService.getAvailableHotelRooms("hotel-1")).thenReturn(rooms);
        when(reviewService.getHotelReviews("hotel-1")).thenReturn(List.of());

        mockMvc.perform(get("/hotels/hotel-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/hotel-details"))
                .andExpect(model().attribute("hotel", hotel))
                .andExpect(model().attribute("rooms", rooms))
                .andExpect(model().attributeExists("reviews"))
                .andExpect(model().attributeExists("user"));

        verify(hotelService).getHotelById("hotel-1");
        verify(roomService).getAvailableHotelRooms("hotel-1");
        verify(reviewService).getHotelReviews("hotel-1");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void hotelDetailsRedirectsWhenHotelDoesNotExist() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("missing-hotel")).thenReturn(null);

        mockMvc.perform(get("/hotels/missing-hotel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));

        verify(roomService, never()).getAvailableHotelRooms(anyString());
        verify(reviewService, never()).getHotelReviews(anyString());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void hotelDetailsRedirectsWhenHotelIsInactive() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("hotel-1")).thenReturn(mockHotel(Hotel.HotelStatus.INACTIVE));

        mockMvc.perform(get("/hotels/hotel-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));

        verify(roomService, never()).getAvailableHotelRooms(anyString());
        verify(reviewService, never()).getHotelReviews(anyString());
    }
}