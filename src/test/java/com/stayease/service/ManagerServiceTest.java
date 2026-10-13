package com.stayease.service;

import com.stayease.model.Booking;
import com.stayease.model.Hotel;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest
{
    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ManagerService managerService;

    private Hotel hotel1;
    private Hotel hotel2;

    @BeforeEach
    void setUp()
    {
        hotel1 = new Hotel();
        hotel1.setHotelId("hotel-1");
        hotel1.setManagerId("manager-1");
        hotel1.setHotelStatus(Hotel.HotelStatus.ACTIVE);

        hotel2 = new Hotel();
        hotel2.setHotelId("hotel-2");
        hotel2.setManagerId("manager-1");
        hotel2.setHotelStatus(Hotel.HotelStatus.INACTIVE);
    }

    @Test
    void getHotelCountByManageIdReturnsCount()
    {
        when(hotelRepository.countByManagerId("manager-1")).thenReturn(2L);

        assertEquals(2L, managerService.getHotelCountByManageId("manager-1"));
        verify(hotelRepository).countByManagerId("manager-1");
    }

    @Test
    void getActiveHotelCountByManageIdReturnsCount()
    {
        when(hotelRepository.countByManagerIdAndHotelStatus("manager-1", Hotel.HotelStatus.ACTIVE)).thenReturn(1L);

        assertEquals(1L, managerService.getActiveHotelCountByManageId("manager-1"));
    }

    @Test
    void getInactiveHotelCountByManageIdReturnsCount()
    {
        when(hotelRepository.countByManagerIdAndHotelStatus("manager-1", Hotel.HotelStatus.INACTIVE)).thenReturn(1L);

        assertEquals(1L, managerService.getInactiveHotelCountByManageId("manager-1"));
    }

    @Test
    void getBookingCountByManageIdReturnsCount()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));
        when(bookingRepository.countByHotelIdIn(List.of("hotel-1", "hotel-2"))).thenReturn(10L);

        assertEquals(10L, managerService.getBookingCountByManageId("manager-1"));
    }

    @Test
    void getBookingCountByManageIdReturnsZeroWhenManagerHasNoHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(0L, managerService.getBookingCountByManageId("manager-1"));
        verify(bookingRepository, never()).countByHotelIdIn(anyList());
    }

    @Test
    void getConfirmedBookingCountByManageIdReturnsCount()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));
        when(bookingRepository.countByHotelIdInAndBookingStatus(List.of("hotel-1", "hotel-2"), Booking.BookingStatus.CONFIRMED)).thenReturn(4L);

        assertEquals(4L, managerService.getConfirmedBookingCountByManageId("manager-1"));
    }

    @Test
    void getConfirmedBookingCountByManageIdReturnsZeroWhenNoHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(0L, managerService.getConfirmedBookingCountByManageId("manager-1"));
        verify(bookingRepository, never()).countByHotelIdInAndBookingStatus(anyList(), any());
    }

    @Test
    void getPendingPaymentCountByManageIdReturnsCount()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));
        when(bookingRepository.countByHotelIdInAndBookingStatus(List.of("hotel-1", "hotel-2"), Booking.BookingStatus.PENDING_PAYMENT)).thenReturn(3L);

        assertEquals(3L, managerService.getPendingPaymentCountByManageId("manager-1"));
    }

    @Test
    void getPendingPaymentCountByManageIdReturnsZeroWhenNoHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(0L, managerService.getPendingPaymentCountByManageId("manager-1"));
        verify(bookingRepository, never()).countByHotelIdInAndBookingStatus(anyList(), any());
    }

    @Test
    void getCancelledBookingCountByManageIdReturnsCount()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));
        when(bookingRepository.countByHotelIdInAndBookingStatus(List.of("hotel-1", "hotel-2"), Booking.BookingStatus.CANCELLED)).thenReturn(2L);

        assertEquals(2L, managerService.getCancelledBookingCountByManageId("manager-1"));
    }

    @Test
    void getCancelledBookingCountByManageIdReturnsZeroWhenNoHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(0L, managerService.getCancelledBookingCountByManageId("manager-1"));
        verify(bookingRepository, never()).countByHotelIdInAndBookingStatus(anyList(), any());
    }

    @Test
    void getCompletedBookingCountByManageIdReturnsCount()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));
        when(bookingRepository.countByHotelIdInAndBookingStatus(List.of("hotel-1", "hotel-2"), Booking.BookingStatus.COMPLETED)).thenReturn(5L);

        assertEquals(5L, managerService.getCompletedBookingCountByManageId("manager-1"));
    }

    @Test
    void getCompletedBookingCountByManageIdReturnsZeroWhenNoHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(0L, managerService.getCompletedBookingCountByManageId("manager-1"));
        verify(bookingRepository, never()).countByHotelIdInAndBookingStatus(anyList(), any());
    }

    @Test
    void getManagerHotelsByManageIdReturnsHotels()
    {
        when(hotelRepository.findByManagerId("manager-1")).thenReturn(List.of(hotel1, hotel2));

        List<Hotel> result = managerService.getManagerHotelsByManageId("manager-1");

        assertEquals(2, result.size());
        assertSame(hotel1, result.get(0));
        assertSame(hotel2, result.get(1));
    }

    @Test
    void isHotelOwnedByManagerReturnsTrueForOwner()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel1));

        assertTrue(managerService.isHotelOwnedByManager("hotel-1", "manager-1"));
    }

    @Test
    void isHotelOwnedByManagerReturnsFalseForDifferentManager()
    {
        when(hotelRepository.findById("hotel-1")).thenReturn(Optional.of(hotel1));

        assertFalse(managerService.isHotelOwnedByManager("hotel-1", "manager-2"));
    }

    @Test
    void isHotelOwnedByManagerThrowsWhenHotelDoesNotExist()
    {
        when(hotelRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> managerService.isHotelOwnedByManager("missing", "manager-1"));

        assertEquals("Hotel not found", exception.getMessage());
    }
}