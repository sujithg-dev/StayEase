package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.model.User;
import com.stayease.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ManagerControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ManagerService managerService;

    @MockitoBean
    private HotelService hotelService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private ImageService imageService;

    private Hotel hotel;
    private Booking booking;

    @BeforeEach
    void setUp()
    {
        User manager = new User();
        manager.setUserId("manager-1");
        manager.setUserName("Test Manager");
        manager.setUserEmail("manager@example.com");

        hotel = new Hotel();
        hotel.setHotelId("hotel-1");
        hotel.setManagerId("manager-1");

        booking = new Booking();
        booking.setBookingId("booking-1");
        booking.setHotelId("hotel-1");

        when(userService.getUserByUserEmail("manager@example.com")).thenReturn(manager);
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void dashboard_returnsDashboardWithStatistics() throws Exception
    {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/dashboard"))
                .andExpect(model().attributeExists(
                        "hotelCount",
                        "activeHotelCount",
                        "inactiveHotelCount",
                        "bookingCount",
                        "confirmedBookingCount",
                        "pendingPaymentCount",
                        "cancelledBookingCount",
                        "completedBookingCount",
                        "currentPage",
                        "user"
                ));

        verify(managerService).getHotelCountByManageId("manager-1");
        verify(managerService).getActiveHotelCountByManageId("manager-1");
        verify(managerService).getInactiveHotelCountByManageId("manager-1");
        verify(managerService).getBookingCountByManageId("manager-1");
        verify(managerService).getConfirmedBookingCountByManageId("manager-1");
        verify(managerService).getPendingPaymentCountByManageId("manager-1");
        verify(managerService).getCancelledBookingCountByManageId("manager-1");
        verify(managerService).getCompletedBookingCountByManageId("manager-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void allManagerHotels_returnsHotelsPage() throws Exception
    {
        when(managerService.getManagerHotelsByManageId("manager-1")).thenReturn(List.of(hotel));

        mockMvc.perform(get("/manager/hotels"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/hotels"))
                .andExpect(model().attributeExists("hotels", "currentPage", "user"));

        verify(managerService).getManagerHotelsByManageId("manager-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void allManagerHotelBookings_returnsBookingsPage() throws Exception
    {
        when(managerService.getManagerHotelsByManageId("manager-1")).thenReturn(List.of(hotel));
        when(bookingService.getHotelBookings("hotel-1")).thenReturn(List.of(booking));

        mockMvc.perform(get("/manager/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/bookings"))
                .andExpect(model().attributeExists("bookings", "manager", "currentPage", "user"));

        verify(bookingService).getHotelBookings("hotel-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void showBookingDetails_whenManagerOwnsHotel_returnsDetailsPage() throws Exception
    {
        when(bookingService.getBookingById("booking-1")).thenReturn(booking);
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);

        mockMvc.perform(get("/manager/bookings/booking-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/booking-details"))
                .andExpect(model().attributeExists("booking", "currentPage", "user"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void showBookingDetails_whenManagerDoesNotOwnHotel_redirectsToBookings() throws Exception
    {
        when(bookingService.getBookingById("booking-1")).thenReturn(booking);
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(get("/manager/bookings/booking-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/bookings"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void showAddHotelPage_returnsAddHotelPage() throws Exception
    {
        mockMvc.perform(get("/manager/hotels/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/add-hotel"))
                .andExpect(model().attributeExists("hotel", "amenities", "currentPage", "user"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void showEditHotelPage_whenManagerOwnsHotel_returnsEditPage() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);
        when(hotelService.getHotelById("hotel-1")).thenReturn(hotel);

        mockMvc.perform(get("/manager/hotels/edit/hotel-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/edit-hotel"))
                .andExpect(model().attributeExists("hotel", "amenities", "currentPage", "user"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void showEditHotelPage_whenManagerDoesNotOwnHotel_throwsException() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(get("/manager/hotels/edit/hotel-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/error"));

        verify(hotelService, never()).getHotelById(anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void addHotel_whenValidationFails_returnsAddHotelPage() throws Exception
    {
        mockMvc.perform(post("/manager/hotels/add")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/add-hotel"))
                .andExpect(model().attributeExists("amenities", "hotel"));

        verify(hotelService, never()).createHotel(any(Hotel.class), anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void updateHotel_whenManagerDoesNotOwnHotel_redirectsToHotels() throws Exception
    {
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(post("/manager/hotels/edit/hotel-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels"));

        verify(hotelService, never()).updateHotel(anyString(), any(Hotel.class), anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void deleteHotel_deletesHotelAndRedirects() throws Exception
    {
        mockMvc.perform(post("/manager/hotels/delete/hotel-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels"));

        verify(hotelService).deleteHotel("hotel-1", "manager-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void updateHotelImage_whenHotelExists_appendsImagesAndRedirects() throws Exception
    {
        hotel.setHotelImages(List.of("/uploads/hotels/old.webp"));
        hotel.setHotelCoverImage("/uploads/hotels/old.webp");

        when(hotelService.getHotelById("hotel-1")).thenReturn(hotel);
        when(imageService.saveHotelImages(any())).thenReturn(List.of("/uploads/hotels/new.webp"));

        mockMvc.perform(multipart("/manager/hotels/edit/image/hotel-1")
                        .with(csrf())
                        .file(new MockMultipartFile("imageFiles", "new.webp", "image/webp", new byte[]{1, 2, 3})))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/edit/hotel-1"));

        verify(hotelService).addHotelImage(eq("hotel-1"), any(Hotel.class), eq("manager-1"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void updateHotelImage_whenHotelDoesNotExist_redirectsToHotels() throws Exception
    {
        when(hotelService.getHotelById("hotel-1")).thenReturn(null);

        mockMvc.perform(multipart("/manager/hotels/edit/image/hotel-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels"));

        verify(hotelService, never()).addHotelImage(anyString(), any(Hotel.class), anyString());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void setHotelCoverImage_setsCoverAndRedirects() throws Exception
    {
        mockMvc.perform(post("/manager/hotels/hotel-1/images/cover")
                        .with(csrf())
                        .param("image", "/uploads/hotels/cover.webp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/edit/hotel-1"));

        verify(hotelService).setHotelCoverImage(
                "hotel-1", "/uploads/hotels/cover.webp", "manager-1"
        );
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void deleteHotelImage_deletesImageAndRedirects() throws Exception
    {
        mockMvc.perform(post("/manager/hotels/hotel-1/images/delete")
                        .with(csrf())
                        .param("image", "/uploads/hotels/image.webp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/hotels/edit/hotel-1"));

        verify(hotelService).deleteHotelImage(
                "hotel-1", "/uploads/hotels/image.webp", "manager-1"
        );
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void cancelBooking_whenManagerOwnsHotel_cancelsAndRedirects() throws Exception
    {
        when(bookingService.getBookingById("booking-1")).thenReturn(booking);
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(true);

        mockMvc.perform(post("/manager/cancel/booking-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/bookings"));

        verify(bookingService).cancelBookingByManager("booking-1", "hotel-1");
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void cancelBooking_whenManagerDoesNotOwnHotel_doesNotCancel() throws Exception
    {
        when(bookingService.getBookingById("booking-1")).thenReturn(booking);
        when(managerService.isHotelOwnedByManager("hotel-1", "manager-1")).thenReturn(false);

        mockMvc.perform(post("/manager/cancel/booking-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/bookings"));

        verify(bookingService, never()).cancelBookingByManager(anyString(), anyString());
    }
}