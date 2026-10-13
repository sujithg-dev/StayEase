package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.dto.BookingRequest;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.Room;
import com.stayease.model.User;
import com.stayease.service.BookingService;
import com.stayease.service.HotelService;
import com.stayease.service.RoomService;
import com.stayease.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BookingControllerTest
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
    private BookingService bookingService;

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

    private Room mockRoom()
    {
        Room room = new Room();
        room.setRoomId("room-1");
        room.setHotelId("hotel-1");
        return room;
    }

    private Booking mockBooking(String bookingId, Booking.BookingStatus status, LocalDate checkInDate)
    {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setBookingStatus(status);
        booking.setCheckInDate(checkInDate);
        booking.setCreatedAt(java.time.LocalDateTime.now());
        return booking;
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void allBookingsReturnsBookingsPage() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(bookingService.getCustomerBookings("customer-1")).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/bookings"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("upcomingBookings"))
                .andExpect(model().attributeExists("otherBookings"));

        verify(bookingService, times(2)).getCustomerBookings("customer-1");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void allBookingsSeparatesUpcomingAndOtherBookings() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        Booking upcoming = mockBooking("booking-1", Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2));
        Booking past = mockBooking("booking-2", Booking.BookingStatus.COMPLETED, LocalDate.now().minusDays(5));

        when(bookingService.getCustomerBookings("customer-1")).thenReturn(List.of(past, upcoming));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("upcomingBookings", List.of(upcoming)))
                .andExpect(model().attribute("otherBookings", List.of(past)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingDetailsReturnsConfirmationPage() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        Booking booking = mockBooking("booking-1", Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2));

        when(bookingService.getCustomerBooking("booking-1", "customer-1")).thenReturn(booking);

        mockMvc.perform(get("/bookings/booking-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-confirmation"))
                .andExpect(model().attribute("booking", booking));

        verify(bookingService).getCustomerBooking("booking-1", "customer-1");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void bookingDetailsPropagatesUnauthorizedBookingError() throws Exception
    {
        User user = mockUser();

        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(user);
        when(bookingService.getCustomerBooking("booking-2", "customer-1")).thenThrow(new RuntimeException("You are not allowed to view this booking"));

        mockMvc.perform(get("/bookings/booking-2"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("error", "You are not allowed to view this booking"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void showBookingFormReturnsFormForActiveHotel() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("hotel-1")).thenReturn(mockHotel(Hotel.HotelStatus.ACTIVE));
        when(roomService.getRoomById("room-1")).thenReturn(mockRoom());

        mockMvc.perform(get("/bookings/create")
                        .param("hotelId", "hotel-1")
                        .param("roomId", "room-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-form"))
                .andExpect(model().attributeExists("hotel"))
                .andExpect(model().attributeExists("room"))
                .andExpect(model().attributeExists("bookingRequest"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void showBookingFormRedirectsWhenRoomBelongsToAnotherHotel() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("hotel-1")).thenReturn(mockHotel(Hotel.HotelStatus.ACTIVE));

        Room room = mockRoom();
        room.setHotelId("hotel-2");
        when(roomService.getRoomById("room-1")).thenReturn(room);

        mockMvc.perform(get("/bookings/create")
                        .param("hotelId", "hotel-1")
                        .param("roomId", "room-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/hotels"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void showBookingFormRedirectsWhenHotelIsInactive() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("hotel-1")).thenReturn(mockHotel(Hotel.HotelStatus.INACTIVE));
        when(roomService.getRoomById("room-1")).thenReturn(mockRoom());

        mockMvc.perform(get("/bookings/create")
                        .param("hotelId", "hotel-1")
                        .param("roomId", "room-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/hotels"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void createBookingDisplaysValidationErrors() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
        when(hotelService.getHotelById("hotel-1")).thenReturn(mockHotel(Hotel.HotelStatus.ACTIVE));
        when(roomService.getRoomById("room-1")).thenReturn(mockRoom());

        mockMvc.perform(post("/bookings/create")
                        .with(csrf())
                        .param("hotelId", "hotel-1")
                        .param("roomId", "room-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-form"))
                .andExpect(model().attributeHasErrors("bookingRequest"));

        verify(bookingService, never()).createBooking(any(BookingRequest.class), anyString(), anyString(), any(User.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void cancelBookingRedirectsToBookingsPage() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        mockMvc.perform(post("/bookings/cancel/booking-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        verify(bookingService).cancelBooking("booking-1", "customer-1");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void cancelBookingStillRedirectsWhenServiceThrowsException() throws Exception
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());

        doThrow(new RuntimeException("Booking cannot be cancelled")).when(bookingService).cancelBooking("booking-1", "customer-1");

        mockMvc.perform(post("/bookings/cancel/booking-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));
    }
}